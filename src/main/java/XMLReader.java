import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class XMLReader {
    private String xmlPath = "TSConfig.xml"; //FIXME Path

    /***
     * Validates the XML File
     * @return true: if Validated; false; else
     */
    public boolean validateXMLSchema(){
        try {
            SchemaFactory factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
            Schema schema = factory.newSchema(new File("src/main/java/TSConfig.xsd"));
            Validator validator = schema.newValidator();
            validator.validate(new StreamSource(new File(xmlPath)));
        } catch (IOException | SAXException ex) {
            System.err.println("Exception: " + ex.getMessage());
            return false;
        }
        return true;
    }

    /**
     * get the whole XML File
     * @return XML file
     */
    private NodeList getNodeList() {
        try {
            File file = new File(xmlPath);

            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();

            DocumentBuilder db = dbf.newDocumentBuilder();
            Document doc = db.parse(file);
            doc.getDocumentElement().normalize();

            return doc.getElementsByTagName("AdaptiveChannelsConfig");
        }
        catch (Exception ex) {
            System.err.println(ex.getMessage());
        }

        return null;
    }

    /**
     * get TSInfo from the XML File
     * @return TSInfo
     */
    private NodeList getTSInfo() {
        Element temp = (Element)getNodeList().item(0);
        return temp.getElementsByTagName("TSInfo");
    }
    /**
     * get ChannelInfo from the XML File
     * @return ChannelInfo
     */
    private NodeList getChannelInfo() {
        Element temp = (Element)getNodeList().item(0);
        return temp.getElementsByTagName("ChannelInfo");
    }
    /**
     * get Hostname
     * @return TeamSpeak Hostname
     */
    public String getTSHost() {
        return ((Element)getTSInfo().item(0)).getElementsByTagName("TSHost").item(0).getTextContent();
    }
    /**
     * get Bot Nickname
     * @return TeamSpeak Bot Nickname
     */
    public String getBotNickname() {
        return ((Element)getTSInfo().item(0)).getElementsByTagName("BotNickname").item(0).getTextContent();
    }
    /**
     * get Username
     * @return TeamSpeak Username
     */
    public String getUsername() {
        return ((Element)((Element)getTSInfo().item(0)).getElementsByTagName("LoginInfo").item(0)).getElementsByTagName("Username").item(0).getTextContent();
    }
    /**
     * get Password
     * @return TeamSpeak Password
     */
    public String getPassword() {
        return ((Element)((Element)getTSInfo().item(0)).getElementsByTagName("LoginInfo").item(0)).getElementsByTagName("Password").item(0).getTextContent();
    }
    /**
     * get Port
     * @return TeamSpeak Port
     */
    public int getPort() {
        if(((Element)getTSInfo().item(0)).getElementsByTagName("TSPort").getLength() > 0) {
            return Integer.valueOf(((Element)getTSInfo().item(0)).getElementsByTagName("TSPort").item(0).getTextContent());
        }
        return 10011;
    }

    /**
     * get Map of all channels
     * @return HashMap of adaptive Channels
     */
    public Map<Integer, List<Integer>> getChannels() {
        Map<Integer, List<Integer>> channelList = new HashMap<>();

        NodeList channels = ((Element)getChannelInfo().item(0)).getElementsByTagName("Channel");

        for(int i = 0; i < channels.getLength(); i++) {
            List<Integer> list = new ArrayList<>();
            int MainChannelID = Integer.parseInt(((Element)channels.item(i)).getElementsByTagName("MainChannelID").item(0).getTextContent());
            int ParentChannelID = Integer.parseInt(((Element)channels.item(i)).getElementsByTagName("ParentChannelID").item(0).getTextContent());
            list.add(MainChannelID);
            channelList.put(ParentChannelID+MainChannelID, list);
        }

        return channelList;
    }
}
