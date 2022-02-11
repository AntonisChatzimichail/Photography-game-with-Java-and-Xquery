/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package my.XqueryApplication;

import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Image;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URI;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.*;
import java.io.FileWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import javax.imageio.ImageIO;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JSeparator;
import javax.swing.SwingUtilities;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.filechooser.FileSystemView;
import javax.xml.namespace.QName;
 
import javax.xml.xquery.XQConnection;
import javax.xml.xquery.XQConstants;
import javax.xml.xquery.XQException;
import javax.xml.xquery.XQItem;
import javax.xml.xquery.XQPreparedExpression;
import javax.xml.xquery.XQSequence;
import javax.xml.xquery.XQStaticContext;
 
import oracle.xml.xquery.OXQConnection;
import oracle.xml.xquery.OXQConstants;
import oracle.xml.xquery.OXQDataSource;
import oracle.xml.xquery.OXQEntity;
import oracle.xml.xquery.OXQEntityKind;
import oracle.xml.xquery.OXQEntityLocator;
import oracle.xml.xquery.OXQEntityResolver;
import oracle.xml.xquery.OXQEntityResolverRequestOptions;
import oracle.xml.xquery.OXQView;
import oracle.xml.xquery.util.FileUtils;

/**
 *
 * @author anton
 */
public class XqueryApplicationUI extends javax.swing.JFrame {

    private String user;
    private String selectedPhoto;
    private static final String PHOTOS_DIR = "photos" + File.separatorChar;
    private static final String XML_DIR = "xml" + File.separatorChar;
    private static final int HEADING_SIZE = 20;
    
    private static class MyEntityResolver extends OXQEntityResolver {
        @Override
        public OXQEntity resolveEntity(OXQEntityKind kind, OXQEntityLocator locator,
                OXQEntityResolverRequestOptions options) throws IOException {
            if (kind == OXQEntityKind.DOCUMENT) {
                URI systemId = locator.getSystemIdAsURI();
                if ("file".equals(systemId.getScheme())) {
                    File file = new File(systemId);
                    FileInputStream input = new FileInputStream(file);
                    OXQEntity result = new OXQEntity(input);
                    result.enlistCloseable(input);
                    return result;
                }
            }
            return null;
        }
    }
    
    private JLabel getPhotoLabel(String username, String filename, String votes){
        ImageIcon imgIcon = new ImageIcon(PHOTOS_DIR+username+File.separatorChar+filename);
        Image image = imgIcon.getImage();
        int height = (image.getHeight(rootPane) > 150) ? 150: image.getHeight(rootPane);
        int width  = height * image.getWidth(rootPane) / image.getHeight(rootPane);
        Image newimg = image.getScaledInstance(width, height,  java.awt.Image.SCALE_SMOOTH);
        imgIcon = new ImageIcon(newimg);

        JLabel label = new JLabel(filename, imgIcon, JLabel.LEFT);
        label.setHorizontalTextPosition(JLabel.CENTER);
        label.setVerticalTextPosition(JLabel.BOTTOM);
        
        if(votes.compareTo("X") != 0){
            label.setText("Votes: "+votes);
        }
        if(votes.compareTo("?") == 0){
            label.setText("");
        }
        
        return label;
    }
    
    private JPanel getSubmitJoinPanel(String challenge){
        JPanel submitJoinPanel = new JPanel();
        submitJoinPanel.setLayout(new BoxLayout(submitJoinPanel, BoxLayout.Y_AXIS));
        
        submitJoinPanel.add(new JLabel("Selected photo: "+selectedPhoto));
        
        JButton submitButton = new JButton();
        submitButton.setText("Submit");
        submitButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                System.out.println(selectedPhoto+" by "+user+" joins "+ challenge);
                String xquery = "declare variable $doc external;\n" +
                        "insert nodes \n" +
                        "   <exist_in fk_filename=\""+selectedPhoto+"\" fk_username=\""+user+"\" fk_title=\""+challenge+"\">\n" +
                        "      <votes>0</votes>\n" +
                        "   </exist_in> \n" +
                        "as last into $doc/exist_in_rel";
                runUpdateXqueryFromString(xquery, XML_DIR+"exist_in.xml");
                
                selectedPhoto = "";
                jScrollPaneLeft.setViewportView(getGoBackPanel());
                jScrollPaneRight.setViewportView(getJoinedChallengesPanel(user));
            }
        });
        submitButton.setEnabled(false);
        submitJoinPanel.add(submitButton);
        
        JButton cancelJoinButton = new JButton();
        cancelJoinButton.setText("Cancel");
        cancelJoinButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                selectedPhoto = "";
                jScrollPaneLeft.setViewportView(getGoBackPanel());
                jScrollPaneRight.setViewportView(getDiscChallengesPanel(user));
            }
        });
        submitJoinPanel.add(cancelJoinButton);
        
        return submitJoinPanel;
    }
    
    private JPanel getPhotosForJoinPanel(){
        JPanel photosForJoinPanel = new JPanel();
        photosForJoinPanel.setLayout(new WrapLayout(FlowLayout.LEFT));
        
        String xquery = "for $x in doc(\""+XML_DIR+"photos.xml\")/photos/photo\n" +
                        "where data($x/@fk_username) eq \""+user+"\"\n" +
                        "return data($x/@filename)";
        ArrayList<String> filenames = runXqueryFromString(xquery);
        
        if(filenames.isEmpty())
            photosForJoinPanel.add(new JLabel("There are no photos in your profile."));
        
        for(String filename: filenames){
            JPanel panel = new JPanel();
            panel.add(getPhotoLabel(user, filename, "X"));
            panel.addMouseListener(new MouseAdapter(){
                @Override
                public void mouseClicked(MouseEvent e){
                    //if photo is single-clicked
                    if(e.getClickCount() == 1){
                        selectedPhoto = filename;
                        Component sjComp = jScrollPaneLeft.getViewport().getView();
                        JPanel sjPanel = (JPanel) sjComp;
                        JLabel sjLabel = (JLabel) sjPanel.getComponent(0);
                        sjLabel.setText("Selected photo: "+selectedPhoto);
                        sjPanel.getComponent(1).setEnabled(true);
                    }
                }
            });
            
            photosForJoinPanel.add(panel);
        }
        
        return photosForJoinPanel;
    }
    
    private JPanel getPhotosPanel(String username){
        JPanel photosPanel = new JPanel();
        photosPanel.setLayout(new WrapLayout(FlowLayout.LEFT));
        
        String xquery = "for $x in doc(\""+XML_DIR+"photos.xml\")/photos/photo\n" +
                        "where data($x/@fk_username) eq \""+username+"\"\n" +
                        "return data($x/@filename)";
        ArrayList<String> filenames = runXqueryFromString(xquery);
        
        if(filenames.isEmpty())
            photosPanel.add(new JLabel("There are no photos in your profile."));
        
        for(String filename: filenames){
            JPanel panel = new JPanel();
            panel.add(getPhotoLabel(username, filename, "X"));
            panel.addMouseListener(new MouseAdapter(){
                @Override
                public void mouseClicked(MouseEvent e){
                    //if photo is single-clicked
                    if(e.getClickCount() == 1){
                        System.out.println("Image "+filename+" pressed!");
                        
                        String xquery = "for $x in doc(\""+XML_DIR+"photos.xml\")/photos/photo\n" +
                        "where data($x/@filename) eq \""+filename+"\"\n" +
                        "and data($x/@fk_username) eq \""+username+"\"\n" +
                        "return data($x/views)";
                        ArrayList<String> res = runXqueryFromString(xquery);
                        String views = res.get(0);
                        
                        xquery = "for $ex in doc(\""+XML_DIR+"exist_in.xml\")/exist_in_rel/exist_in\n"+
                                    "where data($ex/@fk_filename) eq \""+filename+"\"\n" +
                                    "and data($ex/@fk_username) eq \""+username+"\"\n" +
                                    "return data($ex/@fk_title)";
                        res = runXqueryFromString(xquery);
                        int numOfChallenges = res.size();
                        
                        JOptionPane op = new JOptionPane(
                        "Filename: "+filename+"\n"
                       +"Owner: "+username+"\n"
                       +"Views: "+views+"\n"
                       +"No. challenges: "+numOfChallenges+"\n", 
                        JOptionPane.INFORMATION_MESSAGE);
                        op.setVisible(true);
                        if(username.compareTo(user) == 0){
                            Object[] options1 = {"Delete Photo", "OK"};
                            int result = JOptionPane.showOptionDialog(null, 
                                    "Filename: "+filename+"\n"
                                    +"Owner: "+username+"\n"
                                    +"Views: "+views+"\n"
                                    +"No. challenges: "+numOfChallenges+"\n", "Photo Info",
                                    JOptionPane.OK_CANCEL_OPTION, JOptionPane.INFORMATION_MESSAGE,
                                    null, options1, options1[1]);
                            if (result == JOptionPane.OK_OPTION){
                                deletePhoto(filename);
                                System.out.println(filename+" deleted");
                            }
                            
                        }
                        else{
                            JDialog d = op.createDialog("Photo Info");
                            d.setVisible(true);
                            d.dispose();
                        }
                    }
                }
            });
            
            photosPanel.add(panel);
        }
        
        return photosPanel;
    }
    
    private JPanel getProfilePanel(String username){
        JPanel profilePanel = new JPanel();
        profilePanel.setLayout(new BoxLayout(profilePanel, BoxLayout.Y_AXIS));
        
        JLabel playerInfoLabel = new JLabel("Player Info");
        playerInfoLabel.setFont(new Font(playerInfoLabel.getFont().getName(), Font.PLAIN, HEADING_SIZE));
        profilePanel.add(playerInfoLabel);
        profilePanel.add(new JLabel("Username: "+username));
        
        String xquery = 
                "let $players := doc(\""+XML_DIR+"players.xml\")\n" +
                "for $pl in $players/players/player\n" +
                "where data($pl/@username) eq \""+username+"\"\n" +
                "return (data($pl/points), data($pl/level))";
        
        ArrayList<String> res = runXqueryFromString(xquery);
        String points = res.get(0);
        profilePanel.add(new JLabel("Points: "+points));
        String level = res.get(1);
        profilePanel.add(new JLabel("Level: "+level));
        
        profilePanel.add(getAddPhotoButton());
        
        profilePanel.add(getDiscChallengesButton());
        
        profilePanel.add(getJoinedChallengesButton());

        profilePanel.add(signOutButton());
        
        return profilePanel;
    }
    
    private JButton getDiscChallengesButton(){
        JButton discChallengesButton = new JButton();
        discChallengesButton.setText("Discover Challenges");
        discChallengesButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jScrollPaneLeft.setViewportView(getGoBackPanel());
                jScrollPaneRight.setViewportView(getDiscChallengesPanel(user));
            }
        });
        return discChallengesButton;
    }
    
    private JButton getJoinedChallengesButton(){
        JButton joinedChallengesButton = new JButton();
        joinedChallengesButton.setText("Joined Challenges");
        joinedChallengesButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jScrollPaneLeft.setViewportView(getGoBackPanel());
                jScrollPaneRight.setViewportView(getJoinedChallengesPanel(user));
            }
        });
        return joinedChallengesButton;
    }
    
    private void deletePhoto(String filename){
        
        String xquery = "declare variable $doc external;\n" +
                        "for $ph in $doc/photos/photo\n" +
                        "where data($ph/@filename) eq \""+filename+"\" and\n" +
                        "data($ph/@fk_username) eq \""+user+"\"\n" +
                        "return delete nodes $ph";
        runUpdateXqueryFromString(xquery, XML_DIR+"photos.xml");

        xquery = "declare variable $doc external;\n" +
                "for $ex in $doc/exist_in_rel/exist_in\n" +
                "where data($ex/@fk_filename) eq \""+filename+"\" and\n" +
                "data($ex/@fk_username) eq \""+user+"\"\n" +
                "return delete nodes $ex";
        runUpdateXqueryFromString(xquery, XML_DIR+"exist_in.xml");

        Path photoFile = Paths.get(
            PHOTOS_DIR+user+File.separatorChar+filename);
        try {
            Files.delete(photoFile);
        } catch (IOException ex) {
            Logger.getLogger(XqueryApplicationUI.class.getName()).log(Level.SEVERE, null, ex);
        }

        jScrollPaneLeft.setViewportView(getProfilePanel(user));
        jScrollPaneRight.setViewportView(getPhotosPanel(user));
    }
    
    private JButton getAddPhotoButton(){
        JButton addPhotoButton = new JButton();
        addPhotoButton.setText("Add photo");
        addPhotoButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                JFileChooser jfc = new JFileChooser(FileSystemView.getFileSystemView().getHomeDirectory());

		jfc.addChoosableFileFilter(new FileNameExtensionFilter(
                                            "Image files", 
                                            ImageIO.getReaderFileSuffixes()));
                jfc.setAcceptAllFileFilterUsed(false);
                
                int returnValue = jfc.showOpenDialog(null);
                
		if (returnValue != JFileChooser.APPROVE_OPTION)
                    return;
                
                File selectedFile = jfc.getSelectedFile();
                String filename = selectedFile.getName();
                
                String xquery = 
                    "let $photos := doc(\""+XML_DIR+"photos.xml\")\n" +
                    "for $ph in $photos/photos/photo\n" +
                    "where data($ph/@filename) eq \""+filename+"\"\n" +
                    "and data($ph/@fk_username) eq \""+user+"\"\n" +
                    "return data($ph/@filename)";
        
                ArrayList<String> res = runXqueryFromString(xquery);
                
                if(!res.isEmpty()){
                    JOptionPane op = new JOptionPane(
                        "Photo "+filename+" already exists.\n",
                        JOptionPane.ERROR_MESSAGE);
                    op.setVisible(true);
                    JDialog d = op.createDialog("Error in adding photo.");
                    d.setVisible(true);
                    d.dispose();
                    return;
                }
                
                xquery = "declare variable $doc external;\n" +
                        "insert nodes \n" +
                        "   <photo filename=\""+filename+"\" fk_username=\""+user+"\">\n" +
                        "      <views>0</views>\n" +
                        "   </photo> \n" +
                        "as last into $doc/photos";
                runUpdateXqueryFromString(xquery, XML_DIR+"photos.xml");
                
                Path dir = Paths.get(PHOTOS_DIR+user);
                dir.toFile().mkdir();
                
                Path copied = Paths.get(
                    PHOTOS_DIR+user+File.separatorChar+filename);
                Path originalPath = selectedFile.toPath();
                try {
                    Files.copy(originalPath, copied, StandardCopyOption.REPLACE_EXISTING);
                } catch (IOException ex) {
                    Logger.getLogger(XqueryApplicationUI.class.getName()).log(Level.SEVERE, null, ex);
                }
                
                jScrollPaneLeft.setViewportView(getProfilePanel(user));
                jScrollPaneRight.setViewportView(getPhotosPanel(user));
                
                System.out.println(filename+" added");
            }
        });
        return addPhotoButton;
    }
    
    private JPanel getGoBackPanel(){
        JPanel goBackPanel = new JPanel();
        goBackPanel.setLayout(new BoxLayout(goBackPanel, BoxLayout.Y_AXIS));
        goBackPanel.add(myProfileButton());
        goBackPanel.add(getDiscChallengesButton());
        goBackPanel.add(getJoinedChallengesButton());
        goBackPanel.add(signOutButton());
        return goBackPanel;
    }
    
    private JButton myProfileButton(){
        JButton myProfileButton = new JButton();
        myProfileButton.setText("My profile");
        myProfileButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jScrollPaneRight.setViewportView(getPhotosPanel(user));
                jScrollPaneLeft.setViewportView(getProfilePanel(user));
            }
        });
        return myProfileButton;
    }
    
    private JButton signOutButton(){
        JButton signoutButton = new JButton();
        signoutButton.setText("Sign out");
        signoutButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                System.out.println(user+" signed out.");
                user = "?";
                password_field.setText("");
                jScrollPaneLeft.setViewportView(new JPanel());
                jScrollPaneRight.setViewportView(signinPanel);
            }
        });
        return signoutButton;
    }
    
    private JPanel getJoinedChallengesPanel(String username){
        JPanel joinedChalPanel = new JPanel();
        joinedChalPanel.setLayout(new BoxLayout(joinedChalPanel, BoxLayout.Y_AXIS));
        JLabel titleLabel = new JLabel("Joined challenges");
        titleLabel.setFont(new Font(titleLabel.getFont().getName(), Font.PLAIN, HEADING_SIZE));
        joinedChalPanel.add(titleLabel);

        String xquery = "let $exist_in := doc(\""+XML_DIR+"exist_in.xml\")\n" +
            "for $ex in $exist_in/exist_in_rel/exist_in\n" +
            "where data($ex/@fk_username) eq \""+username+"\"\n" +
            "order by $ex/votes descending\n" +
            "return (data($ex/@fk_title), data($ex/votes))";
        ArrayList<String> joinedChalRes = runXqueryFromString(xquery);
        if(joinedChalRes.isEmpty()){
            joinedChalPanel.add(new JLabel("You have not joined any challenge"));
        }
        for(int i=0; i<joinedChalRes.size(); i+=2){
            String chal = joinedChalRes.get(i);
            String votes = joinedChalRes.get(i+1);
            JButton chalButton = new JButton();
            chalButton.setText(chal);
            chalButton.addActionListener(new java.awt.event.ActionListener() {
                public void actionPerformed(java.awt.event.ActionEvent evt) {
                    jScrollPaneLeft.setViewportView(getGoBackPanel());
                    jScrollPaneRight.setViewportView(getChallengePanel(chal, votes));
                }
            });
            joinedChalPanel.add(chalButton);
            joinedChalPanel.add(new JLabel("Votes: "+votes));
        }
        return joinedChalPanel;
    }
    
    private JPanel getDiscChallengesPanel(String username){
        JPanel discChalPanel = new JPanel();
        discChalPanel.setLayout(new BoxLayout(discChalPanel, BoxLayout.Y_AXIS));
        JLabel titleLabel = new JLabel("Discover challenges");
        titleLabel.setFont(new Font(titleLabel.getFont().getName(), Font.PLAIN, HEADING_SIZE));
        discChalPanel.add(titleLabel);

        String xquery = "let $takes_part:= (\n" +
            "let $exist_in := doc(\""+XML_DIR+"exist_in.xml\")\n" +
            "for $ex in $exist_in/exist_in_rel/exist_in\n" +
            "where data($ex/@fk_username) eq \""+username+"\"\n" +
            "return $ex/@fk_title/string()\n" +
            ")\n" +
            "return doc(\""+XML_DIR+"challenges.xml\")/challenges/challenge/@title/string()[not(.=$takes_part)]";
        ArrayList<String> discChalRes = runXqueryFromString(xquery);
        if(discChalRes.isEmpty()){
            discChalPanel.add(new JLabel("You have joined all challenges"));
        }
        for(String chal: discChalRes){
            JButton chalButton = new JButton();
            chalButton.setText(chal);
            chalButton.addActionListener(new java.awt.event.ActionListener() {
                public void actionPerformed(java.awt.event.ActionEvent evt) {
                    jScrollPaneLeft.setViewportView(getGoBackPanel());
                    jScrollPaneRight.setViewportView(getChallengePanel(chal, "X"));
                }
            });
            discChalPanel.add(chalButton);
        }
        return discChalPanel;
    }
    
    private JPanel getLeaderboardPanel(String challenge){
        JPanel leaderboardPanel = new JPanel();
        leaderboardPanel.setLayout(new BoxLayout(leaderboardPanel, BoxLayout.Y_AXIS));
        
        JLabel titleLabel = new JLabel("Leaderboard for "+challenge);
        titleLabel.setFont(new Font(titleLabel.getFont().getName(), Font.PLAIN, HEADING_SIZE));
        leaderboardPanel.add(titleLabel);
        
        String xquery = "let $exist_in := doc(\""+XML_DIR+"exist_in.xml\")\n" +
                        "for $ex in $exist_in/exist_in_rel/exist_in\n" +
                        "where data($ex/@fk_title) eq \""+challenge+"\"\n" +
                        "order by $ex/votes descending\n" +
                        "return (data($ex/@fk_filename), data($ex/@fk_username), data($ex/votes) )";
        
        ArrayList<String> res = runXqueryFromString(xquery);
        
        if(res.isEmpty()){
            leaderboardPanel.add(new JLabel("No players in challenge."));
        }
        
        for(int i=0; i<res.size(); i+=3){
            String filename = res.get(i);
            String username = res.get(i+1);
            String votes = res.get(i+2);
            leaderboardPanel.add(new JLabel((i/3+1)+". Votes: "+votes+", "+filename+" by "+username));
        }
        
        return leaderboardPanel;
    }
    
    private JPanel getChallengePanel(String challenge, String votes){
        JPanel challengePanel = new JPanel();
        challengePanel.setLayout(new BoxLayout(challengePanel, BoxLayout.Y_AXIS));
        
        String xquery = 
            "let $challenges := doc(\""+XML_DIR+"challenges.xml\")\n" +
            "for $ch in $challenges/challenges/challenge\n" +
            "where data($ch/@title) eq \""+challenge+"\"\n" +
            "return (data($ch/description), data($ch/end_date))";
            
        ArrayList<String> chalRes = runXqueryFromString(xquery);
        String description = chalRes.get(0);
        String endDate = chalRes.get(1);
        
        JLabel titleLabel = new JLabel(challenge);
        titleLabel.setFont(new Font(titleLabel.getFont().getName(), Font.PLAIN, HEADING_SIZE));
        challengePanel.add(titleLabel);
        challengePanel.add(new JLabel(description));
        challengePanel.add(new JLabel("End date: "+endDate));
        
        if(votes.compareTo("X") != 0){
            xquery = "let $exist_in := doc(\""+XML_DIR+"exist_in.xml\")\n" +
            "for $ex in $exist_in/exist_in_rel/exist_in\n" +
            "where data($ex/@fk_username) eq \""+user+"\"\n" +
            "and data($ex/@fk_title) eq \""+challenge+"\"\n" +
            "return data($ex/@fk_filename)";
            
            ArrayList<String> res = runXqueryFromString(xquery);
            String filename = res.get(0);
            
            challengePanel.add(new JLabel("Your photo:"));
            challengePanel.add(getPhotoLabel(user, filename, votes));
            JButton voteButton = new JButton();
            voteButton.setText("Vote!");
            voteButton.addActionListener(new java.awt.event.ActionListener() {
                public void actionPerformed(java.awt.event.ActionEvent evt) {
                    ArrayList<String> votePhotoInfo = getVotePhotoInfo(challenge);
                    jScrollPaneLeft.setViewportView(getSubmitVotesPanel(challenge, votes, votePhotoInfo));
                    jScrollPaneRight.setViewportView(getVotePanel(challenge, votePhotoInfo));
                }
            });
            challengePanel.add(voteButton);
        }
        else{
            JButton joinButton = new JButton();
            joinButton.setText("Join!");
            joinButton.addActionListener(new java.awt.event.ActionListener() {
                public void actionPerformed(java.awt.event.ActionEvent evt) {
                    jScrollPaneLeft.setViewportView(getSubmitJoinPanel(challenge));
                    jScrollPaneRight.setViewportView(getPhotosForJoinPanel());
                }
            });
            challengePanel.add(joinButton);
        }
        
        JButton leaderboardButton = new JButton();
        leaderboardButton.setText("Show leaderboard");
        leaderboardButton.addActionListener(new java.awt.event.ActionListener() {
                public void actionPerformed(java.awt.event.ActionEvent evt) {
                    jScrollPaneLeft.setViewportView(getGoBackPanel());
                    jScrollPaneRight.setViewportView(getLeaderboardPanel(challenge));
                }
            });
        challengePanel.add(leaderboardButton);
        
        return challengePanel;
    }
    
    private ArrayList<String> getVotePhotoInfo(String challenge){
        String xquery = "let $exist_in := doc(\""+XML_DIR+"exist_in.xml\")\n" +
                        "for $ex in $exist_in/exist_in_rel/exist_in\n" +
                        "where data($ex/@fk_title) eq \""+challenge+"\"\n" +
                        "and data($ex/@fk_username) ne \""+user+"\"\n" +
                        "return ( data($ex/@fk_filename), data($ex/@fk_username) )";
        return runXqueryFromString(xquery);
    }
    
    private JPanel getVotePanel(String challenge, ArrayList<String> votePhotoInfo){
        JPanel votePanel = new JPanel();
        votePanel.setLayout(new WrapLayout(FlowLayout.LEFT));
        
        if(votePhotoInfo.isEmpty()){
            votePanel.add(new JLabel("There are no photos from other players in this challenge."));
        }
        
        for(int i=0; i<votePhotoInfo.size(); i+=2){
            String filename = votePhotoInfo.get(i);
            String username = votePhotoInfo.get(i+1);
            
            JPanel panel = new JPanel();
            panel.addMouseListener(new MouseAdapter(){
                @Override
                public void mouseClicked(MouseEvent e){
                    //if photo is single-clicked
                    if(e.getClickCount() == 1){
                        if(panel.getComponent(0).isEnabled()){
                            panel.getComponent(0).setEnabled(false);
                            JLabel newLabel = (JLabel)panel.getComponent(0);
                            newLabel.setText("Voted!");
                        }
                        else{
                            panel.getComponent(0).setEnabled(true);
                            JLabel newLabel = (JLabel)panel.getComponent(0);
                            newLabel.setText("");
                        }
                    }
                }
            });
            panel.add(getPhotoLabel(username, filename, "?"));
            
            String xquery = "declare variable $doc external;\n" +
                            "for $ph in $doc/photos/photo\n" +
                            "where data($ph/@filename) eq \""+filename+"\" and\n" +
                            "data($ph/@fk_username) eq \""+username+"\"\n" +
                            "return replace value of node $ph/views\n" +
                            "with xs:int($ph/views)+1";
            runUpdateXqueryFromString(xquery, XML_DIR+"photos.xml");
            
            votePanel.add(panel);
        }
        return votePanel;
    }
    
    private JPanel getSubmitVotesPanel(String challenge, String myVotes, ArrayList<String> votePhotoInfo){
        JPanel submitVotesPanel = new JPanel();
        submitVotesPanel.setLayout(new BoxLayout(submitVotesPanel, BoxLayout.Y_AXIS));
        
        JButton submitVotesButton = new JButton();
        submitVotesButton.setText("Submit votes!");
        submitVotesButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                if(!votePhotoInfo.isEmpty()){
                    Component voteComp = jScrollPaneRight.getViewport().getView();
                    JPanel votePanel = (JPanel) voteComp;
                    Component[] photoComponents = votePanel.getComponents();
                    for(int i=0; i<photoComponents.length; i++){
                        JPanel photoPanel = (JPanel) photoComponents[i];
                        boolean isVoted = !photoPanel.getComponent(0).isEnabled();
                        if(isVoted){
                            String filename = votePhotoInfo.get(2*i);
                            String username = votePhotoInfo.get(2*i+1);
                            String xquery = "declare variable $doc external;\n" +
                                            "for $ex in $doc/exist_in_rel/exist_in\n" +
                                            "where data($ex/@fk_filename) eq \""+filename+"\" and\n" +
                                            "data($ex/@fk_username) eq \""+username+"\" and\n" +
                                            "data($ex/@fk_title) eq \""+challenge+"\"\n" +
                                            "return replace value of node $ex/votes\n" +
                                            "with xs:int($ex/votes)+1";
                            runUpdateXqueryFromString(xquery, XML_DIR+"exist_in.xml");
                            
                            System.out.println(filename+" by "+username+" in "+challenge+" just voted!");
                        }
                    }
                }
                jScrollPaneLeft.setViewportView(getGoBackPanel());
                jScrollPaneRight.setViewportView(getChallengePanel(challenge, myVotes));
            }
        });
        submitVotesPanel.add(submitVotesButton);
        
        return submitVotesPanel;
    }
    
    private static void runUpdateXqueryFromString(String xquery, String filenameXML){
        FileWriter myWriter = null;
        try {
            myWriter = new FileWriter("run.xq");
            myWriter.write(xquery);
            myWriter.close();
            
            runUpdateXqueryFromFile("run.xq", filenameXML);
        } catch (IOException ex) {
            Logger.getLogger(XqueryApplicationUI.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            try {
                myWriter.close();
            } catch (IOException ex) {
                Logger.getLogger(XqueryApplicationUI.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }
    
    private static void runUpdateXqueryFromFile(String filenameXq, String filenameXML){
        try {
            OXQDataSource ds = new OXQDataSource();
            XQConnection con = ds.getConnection();
            
            XQStaticContext ctx = con.getStaticContext();
            // Set the binding mode to deferred so the document
            // item is not copied when it is bound.
            ctx.setBindingMode(XQConstants.BINDING_MODE_DEFERRED);
            con.setStaticContext(ctx);
            
            FileInputStream input = new FileInputStream(filenameXML);
            XQItem doc = con.createItemFromDocument(input, null, null);
            input.close();
            
            //System.out.println("Before update: \n" + doc.getItemAsString(null));
            
            FileInputStream query = new FileInputStream(filenameXq);
            XQPreparedExpression expr = con.prepareExpression(query);
            query.close();
            expr.bindItem(new QName("doc"), doc);
            // Enable updates (disabled by default)
            OXQView.getDynamicContext(expr).setUpdateMode(OXQConstants.UPDATE_MODE_ENABLED);
            expr.executeQuery();
            
            //System.out.println("After update: \n" + doc.getItemAsString(null));
            
            // Write the modified document back to the file
            FileOutputStream out = new FileOutputStream(filenameXML);
            doc.writeItem(out, null);
            
            expr.close();
            con.close();
        } catch (XQException ex) {
            Logger.getLogger(XqueryApplicationUI.class.getName()).log(Level.SEVERE, null, ex);
        } catch (FileNotFoundException ex) {
            Logger.getLogger(XqueryApplicationUI.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(XqueryApplicationUI.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    private static ArrayList<String> runXqueryFromString(String xquery){
        ArrayList<String> res = new ArrayList<>();
        FileWriter myWriter = null;
        try {
            myWriter = new FileWriter("run.xq");
            myWriter.write(xquery);
            myWriter.close();
            
            return runXqueryFromFile("run.xq");
        } catch (IOException ex) {
            Logger.getLogger(XqueryApplicationUI.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            try {
                myWriter.close();
            } catch (IOException ex) {
                Logger.getLogger(XqueryApplicationUI.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        return res;
    }
    
    private static ArrayList<String> runXqueryFromFile(String filename){
        ArrayList<String> res = new ArrayList<>();
        
        try {
            OXQDataSource ds = new OXQDataSource();
            XQConnection con = ds.getConnection();
            
            // OXQView is used to access Oracle extensions on XQJ objects.
            OXQConnection ocon = OXQView.getConnection(con);
            ocon.setEntityResolver(new MyEntityResolver());
            
            File query = new File(filename);
            
            // Relative URIs are resolved against the base URI before invoking the entity resolver.
            // The relative URI 'books.xml' used in the query will be resolved against this URI.
            XQStaticContext ctx = con.getStaticContext();
            ctx.setBaseURI(query.toURI().toString());
            
            FileInputStream queryInput = new FileInputStream(query);
            XQPreparedExpression expr = con.prepareExpression(queryInput, ctx);
            queryInput.close();
            XQSequence result = expr.executeQuery();
            while (result.next()){
                res.add(result.getAtomicValue());
            }
            
            result.close();
            expr.close();
            con.close();
        } catch (XQException | IOException ex) {
            Logger.getLogger(XqueryApplicationUI.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        return res;
    }
    
    private boolean signUp(String username, String password){
        
        if(password.compareTo("") == 0)
            return false;
        
        String xquery = 
                "let $players := doc(\""+XML_DIR+"players.xml\")\n" +
                "for $pl in $players/players/player\n" +
                "where data($pl/@username) eq \""+username+"\"\n" +
                "return data($pl/@username)";
        
        ArrayList<String> res = runXqueryFromString(xquery);
        if(!res.isEmpty())
            return false;
        
        xquery = "declare variable $doc external;\n" +
                "insert nodes \n" +
                "   <player username=\""+username+"\">\n" +
                "      <password>"+password+"</password>\n" +
                "      <points>0</points>\n" +
                "      <level>1</level>\n" +
                "   </player> \n" +
                "as last into $doc/players";
                runUpdateXqueryFromString(xquery, XML_DIR+"players.xml");
        
        return true;
    }
    
    private boolean match_signin(String username, String password){
        String xquery_password;
        String xquery = 
                "let $players := doc(\""+XML_DIR+"players.xml\")\n" +
                "for $pl in $players/players/player\n" +
                "where data($pl/@username) eq \""+username+"\"\n" +
                "return data($pl/password)";
        
        ArrayList<String> res = runXqueryFromString(xquery);
        if(res.isEmpty())
            return false;
        
        xquery_password = res.get(0);
        if(xquery_password.compareTo(password)!=0)
            return false;
        user = username;
        return true;
    }
    
    /**
     * Creates new form XqueryApplicationUI
     */
    public XqueryApplicationUI() {
        initComponents();
        
        user = "?";
        selectedPhoto = "";
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jScrollPaneLeft = new javax.swing.JScrollPane();
        jScrollPaneRight = new javax.swing.JScrollPane();
        signinPanel = new javax.swing.JPanel();
        jLabel3 = new javax.swing.JLabel();
        jLabel1 = new javax.swing.JLabel();
        username_field = new javax.swing.JTextField();
        jLabel2 = new javax.swing.JLabel();
        password_field = new javax.swing.JPasswordField();
        signinButton = new javax.swing.JButton();
        signUpButton = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);

        jLabel3.setFont(new java.awt.Font("Tahoma", 0, 24)); // NOI18N
        jLabel3.setText("Player login");

        jLabel1.setText("Username:");

        username_field.setText("achatzimichail");

        jLabel2.setText("Password:");

        signinButton.setText("Sign in");
        signinButton.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        signinButton.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                signinButtonMouseClicked(evt);
            }
        });

        signUpButton.setText("Sign up");
        signUpButton.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        signUpButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                signUpButtonActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout signinPanelLayout = new javax.swing.GroupLayout(signinPanel);
        signinPanel.setLayout(signinPanelLayout);
        signinPanelLayout.setHorizontalGroup(
            signinPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, signinPanelLayout.createSequentialGroup()
                .addContainerGap(162, Short.MAX_VALUE)
                .addGroup(signinPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(signinPanelLayout.createSequentialGroup()
                        .addGap(79, 79, 79)
                        .addComponent(signinButton)
                        .addGap(18, 18, 18)
                        .addComponent(signUpButton))
                    .addGroup(signinPanelLayout.createSequentialGroup()
                        .addGap(86, 86, 86)
                        .addComponent(jLabel3))
                    .addGroup(signinPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                        .addComponent(username_field)
                        .addComponent(password_field, javax.swing.GroupLayout.PREFERRED_SIZE, 300, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(jLabel1)
                        .addComponent(jLabel2)))
                .addContainerGap(141, Short.MAX_VALUE))
        );
        signinPanelLayout.setVerticalGroup(
            signinPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(signinPanelLayout.createSequentialGroup()
                .addContainerGap(122, Short.MAX_VALUE)
                .addComponent(jLabel3)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jLabel1)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(username_field, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jLabel2)
                .addGap(5, 5, 5)
                .addComponent(password_field, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(signinPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(signinButton)
                    .addComponent(signUpButton))
                .addContainerGap(125, Short.MAX_VALUE))
        );

        jScrollPaneRight.setViewportView(signinPanel);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addComponent(jScrollPaneLeft, javax.swing.GroupLayout.PREFERRED_SIZE, 200, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPaneRight))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jScrollPaneLeft)
            .addComponent(jScrollPaneRight)
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void signinButtonMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_signinButtonMouseClicked
        System.out.println("Try to sign in for "+ username_field.getText());
        if(match_signin(username_field.getText(),new String(password_field.getPassword()))){
            jScrollPaneRight.setViewportView(getPhotosPanel(user));
            jScrollPaneLeft.setViewportView(getProfilePanel(user));
            System.out.println(user + " signed in.");
        }
        else{
            password_field.setText("");
            JOptionPane op = new JOptionPane(
                        "Incorrect username or password.\n",
                        JOptionPane.ERROR_MESSAGE);
            op.setVisible(true);
            JDialog d = op.createDialog("Sign in failed");
            d.setVisible(true);
            d.dispose();
            System.out.println("Sign in failed.");
        }
    }//GEN-LAST:event_signinButtonMouseClicked

    private void signUpButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_signUpButtonActionPerformed
        System.out.println("Try to sign up for "+ username_field.getText());
        if(signUp(username_field.getText(),new String(password_field.getPassword()))){
            System.out.println(username_field.getText() + " signed up.");
            password_field.setText("");
            JOptionPane op = new JOptionPane(
                        "Sign up successful for \""+username_field.getText()+"\".\n"
                                + "You can now sign in.",
                        JOptionPane.INFORMATION_MESSAGE);
            op.setVisible(true);
            JDialog d = op.createDialog("Sign up successful");
            d.setVisible(true);
            d.dispose();
        }
        else{
            System.out.println("Sign up failed.");
            password_field.setText("");
            JOptionPane op = new JOptionPane(
                        "Username \""+username_field.getText()+"\" already exists "
                                + "or password field left blank.\n",
                        JOptionPane.ERROR_MESSAGE);
            op.setVisible(true);
            JDialog d = op.createDialog("Sign up failed");
            d.setVisible(true);
            d.dispose();
        }
    }//GEN-LAST:event_signUpButtonActionPerformed

    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        /* Set the Nimbus look and feel */
        //<editor-fold defaultstate="collapsed" desc=" Look and feel setting code (optional) ">
        /* If Nimbus (introduced in Java SE 6) is not available, stay with the default look and feel.
         * For details see http://download.oracle.com/javase/tutorial/uiswing/lookandfeel/plaf.html 
         */
        try {
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (ClassNotFoundException ex) {
            java.util.logging.Logger.getLogger(XqueryApplicationUI.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            java.util.logging.Logger.getLogger(XqueryApplicationUI.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            java.util.logging.Logger.getLogger(XqueryApplicationUI.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(XqueryApplicationUI.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                new XqueryApplicationUI().setVisible(true);
            }
        });
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JScrollPane jScrollPaneLeft;
    private javax.swing.JScrollPane jScrollPaneRight;
    private javax.swing.JPasswordField password_field;
    private javax.swing.JButton signUpButton;
    private javax.swing.JButton signinButton;
    private javax.swing.JPanel signinPanel;
    private javax.swing.JTextField username_field;
    // End of variables declaration//GEN-END:variables
}
