import com.github.theholywaffle.teamspeak3.TS3Api;
import com.github.theholywaffle.teamspeak3.TS3Config;
import com.github.theholywaffle.teamspeak3.TS3Query;
import com.github.theholywaffle.teamspeak3.api.ChannelProperty;
import com.github.theholywaffle.teamspeak3.api.event.ChannelCreateEvent;
import com.github.theholywaffle.teamspeak3.api.event.ChannelDeletedEvent;
import com.github.theholywaffle.teamspeak3.api.event.ChannelDescriptionEditedEvent;
import com.github.theholywaffle.teamspeak3.api.event.ChannelEditedEvent;
import com.github.theholywaffle.teamspeak3.api.event.ChannelMovedEvent;
import com.github.theholywaffle.teamspeak3.api.event.ChannelPasswordChangedEvent;
import com.github.theholywaffle.teamspeak3.api.event.ClientJoinEvent;
import com.github.theholywaffle.teamspeak3.api.event.ClientLeaveEvent;
import com.github.theholywaffle.teamspeak3.api.event.ClientMovedEvent;
import com.github.theholywaffle.teamspeak3.api.event.PrivilegeKeyUsedEvent;
import com.github.theholywaffle.teamspeak3.api.event.ServerEditedEvent;
import com.github.theholywaffle.teamspeak3.api.event.TS3EventType;
import com.github.theholywaffle.teamspeak3.api.event.TS3Listener;
import com.github.theholywaffle.teamspeak3.api.event.TextMessageEvent;
import com.github.theholywaffle.teamspeak3.api.wrapper.Channel;
import com.github.theholywaffle.teamspeak3.api.wrapper.Permission;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Main {
    //Info of all TS Channels
    private static List<Channel> channelList;

    private static Map<Integer, List<Integer>> channels;
    private static TS3Api api;

    private static XMLReader xmlReader = new XMLReader();

    private static int getParentID(int sumID, int mainChannel) {
        return sumID-mainChannel;
    }

    /**
     * Updates Channel List.
     */
    private static void updateChannelList(){
        channelList = api.getChannels();
    }

    /**
     * Tests in the channelList if given channel is empty.
     * @param cid Channel ID
     * @return true: if channel is empty or not existing; else: false
     */
    private static boolean ChannelIsEmpty(int cid){
        for(Channel ch: channelList) {
            if(ch.getId() == cid){
                return (ch.getTotalClients() == 0);
            }
        }
        return true;
    }

    /**
     * removes all Spacers from a String
     * @param chName String to remove
     * @return clear String
     */
    private static String removeSpacer(String chName) {
        if (! xmlReader.isSpacerSelected())
            return chName;

        chName = chName.replace(xmlReader.getTopSpacer(), "");
        chName = chName.replace(xmlReader.getMiddleSpacer(), "");
        chName = chName.replace(xmlReader.getBottomSpacer(), "");
        chName = chName.replace("\u200B", "");

        return chName;
    }

    /**
     * Repeats a String multiple times
     * @param input string
     * @param times repeat
     * @return repeated String
     */
    private static String repeat(String input, int times) {
        String out = "";
        for(int i = 0; i < times; i++) {
            out += input;
        }

        return out;
    }

    /**
     * Checks if the given channel name exists on the TeamSpeak
     * @param involvedChannels all Channels
     * @param curName channel name
     * @return true: if exists; false: else
     */
    private static boolean nameExists(List<Integer> involvedChannels, String curName) {
        for(int chID: involvedChannels) {
            if(api.getChannelInfo(chID).getName().equals(curName))
                return true;
        }
        return false;
    }

    /**
     * Startup routine
     * Detects already existing adaptive channels, add them to the list and corrects the Name
     */
    public static void startUp() {
        updateChannelList();
        channels = xmlReader.getChannels();

        //Detect already existing adaptive channels and add them to the list
        for(int chID: channels.keySet()) {
            String chName = api.getChannelInfo(channels.get(chID).get(0)).getName();
            chName = removeSpacer(chName);

            for(Channel ch: channelList) {
                String tempName = ch.getName();
                tempName = removeSpacer(tempName);

                if(chName.equals(tempName) && ch.getId() != channels.get(chID).get(0)) {
                    channels.get(chID).add(ch.getId());
                }
            }
        }

        //Rename channels
        for(int chID: channels.keySet()) {
            Map<Integer, String> names = new HashMap<>();

            for(int i = 0; i < channels.get(chID).size(); i++) {
                String newName = api.getChannelInfo(channels.get(chID).get(i)).getName();
                newName = newName.replace("\u200B", "");

                names.put(channels.get(chID).get(i), newName.substring(0,1) + repeat("\u200B", i) + newName.substring(1));

            }

            //Edit channel names if necessary
            int breakCounter = 0;
            while(names.size() > 0) {
                breakCounter++;
                for(int i = 0; i < channels.get(chID).size(); i++) {
                    if(!api.getChannelInfo(channels.get(chID).get(i)).getName().equals(names.get(channels.get(chID).get(i)))) {
                        if(!nameExists(channels.get(chID), names.get(channels.get(chID).get(i)))){
                            Map<ChannelProperty, String> newProperty = new HashMap<>();
                            newProperty.put(ChannelProperty.CHANNEL_NAME, names.get(channels.get(chID).get(i)));
                            api.editChannel(channels.get(chID).get(i), newProperty);
                        }

                        names.remove(channels.get(chID).get(i));
                        breakCounter = 0;
                    } else {
                        names.remove(channels.get(chID).get(i));
                    }
                }

                if(breakCounter > 1) {
                    System.out.println("WARNING: The Teamspeak contains simmilar channel Names: " + names.get(chID));
                }
            }

        }

        System.out.println(channels);
    }

    /**
     * Updates channels.
     * All empty channels (except of one) of a channel group will be removed.
     * If a channel group is full a new channel in this group will be generated.
     */
    private static void updateChannels(){
        try{
            for (Map.Entry<Integer, List<Integer>> entry : channels.entrySet()) {
                //Delete empty channels
                for(int i = 1; i < entry.getValue().size()-1; i++) {
                    if(ChannelIsEmpty(entry.getValue().get(i))) {
                        System.out.println("Deleted " + entry.getValue().get(i));
                        api.deleteChannel(entry.getValue().get(i));
                        entry.getValue().remove(i);
                        i--;

                        if(api.getChannelInfo(entry.getValue().get(entry.getValue().size()-1)).getName().substring(0,1).equals(xmlReader.getBottomSpacer())) {
                            Map<ChannelProperty, String> oldProperty = new HashMap<>();
                            oldProperty.put(ChannelProperty.CHANNEL_NAME, xmlReader.getBottomSpacer() + "\u200B" + api.getChannelInfo(entry.getValue().get(entry.getValue().size()-1)).getName().substring(1));
                            api.editChannel(entry.getValue().get(entry.getValue().size()-1), oldProperty);
                        }
                    }
                }

                //Create channel
                if(!ChannelIsEmpty(entry.getValue().get(0)) && !ChannelIsEmpty(entry.getValue().get(entry.getValue().size()-1))) {
                    //Copy properties from Main Channel
                    final Map<ChannelProperty, String> properties = new HashMap<>();
                    properties.put(ChannelProperty.CHANNEL_FLAG_SEMI_PERMANENT, "1");
                    properties.put(ChannelProperty.CPID, String.valueOf(getParentID(entry.getKey(), entry.getValue().get(0))));
                    properties.put(ChannelProperty.CHANNEL_ORDER, String.valueOf(entry.getValue().get(entry.getValue().size()-1)));
                    properties.put(ChannelProperty.CHANNEL_CODEC_QUALITY, "10");

                    if(api.getChannelInfo(entry.getValue().get(0)).getMaxClients() > 0) {
                        properties.put(ChannelProperty.CHANNEL_MAXCLIENTS, String.valueOf(api.getChannelInfo(entry.getValue().get(0)).getMaxClients()));
                        properties.put(ChannelProperty.CHANNEL_FLAG_MAXCLIENTS_UNLIMITED, "0");
                    }

                    //Edit channel name if Spacer are selected
                    String channelName;
                    if(xmlReader.isSpacerSelected()) {
                        String mainSpacer = api.getChannelInfo(entry.getValue().get(entry.getValue().size()-1)).getName().substring(0,1);

                        if(!mainSpacer.equals(xmlReader.getBottomSpacer())) {
                            channelName = xmlReader.getMiddleSpacer()+ "\u200B" + api.getChannelInfo(entry.getValue().get(entry.getValue().size()-1)).getName().substring(1);
                        } else {
                            Map<ChannelProperty, String> oldProperty = new HashMap<>();
                            oldProperty.put(ChannelProperty.CHANNEL_NAME, xmlReader.getMiddleSpacer() + api.getChannelInfo(entry.getValue().get(entry.getValue().size()-1)).getName().substring(1));
                            api.editChannel(entry.getValue().get(entry.getValue().size()-1), oldProperty);

                            channelName = xmlReader.getBottomSpacer() + "\u200B" + api.getChannelInfo(entry.getValue().get(entry.getValue().size()-1)).getName().substring(1);
                        }
                    } else {
                        channelName = xmlReader.getMiddleSpacer() + "\u200B" + api.getChannelInfo(entry.getValue().get(entry.getValue().size()-1)).getName().substring(1);
                    }

                    int createID = api.createChannel(channelName, properties);

                    //Copy permissions from main channel
                    List<Permission> permissions = api.getChannelPermissions(entry.getValue().get(0));
                    for(Permission elem: permissions) {
                        api.addChannelPermission(createID, elem.getName(), elem.getValue());
                    }

                    System.out.println("Created " + createID);
                    entry.getValue().add(createID);
                }

                //Delete last channel if main channel is empty
                if(ChannelIsEmpty(entry.getValue().get(entry.getValue().size()-1)) && ChannelIsEmpty(entry.getValue().get(0)) && entry.getValue().size() > 1) {
                    if(api.getChannelInfo(entry.getValue().get(entry.getValue().size()-1)).getName().substring(0,1).equals(xmlReader.getBottomSpacer())) {
                        Map<ChannelProperty, String> oldProperty = new HashMap<>();
                        oldProperty.put(ChannelProperty.CHANNEL_NAME, xmlReader.getBottomSpacer() + api.getChannelInfo(entry.getValue().get(entry.getValue().size()-2)).getName().substring(1));
                        api.editChannel(entry.getValue().get(entry.getValue().size()-2), oldProperty);
                    }

                    System.out.println("Deleted " + entry.getValue().get(entry.getValue().size()-1));
                    api.deleteChannel(entry.getValue().get(entry.getValue().size()-1));
                    entry.getValue().remove(entry.getValue().size()-1);
                }
            }
        } catch(Exception ex) {
            System.err.println(ex.getMessage());
        }
    }

    public static void main(String ... args) {
        if(!xmlReader.validateXMLSchema()) {
            System.out.println("XML File is not valid");
            return;
        }

        TS3Config config = new TS3Config();
        config.setHost(xmlReader.getTSHost());
        config.setFloodRate(TS3Query.FloodRate.custom(100));
        config.setQueryPort(xmlReader.getPort());

        TS3Query query = new TS3Query(config);
        query.connect();

        api = query.getApi();
        api.login(xmlReader.getUsername(), xmlReader.getPassword());
        api.selectVirtualServerById(1);
        api.setNickname(xmlReader.getBotNickname());

        startUp();

        System.out.println("RUNNING");


        api.registerEvent(TS3EventType.CHANNEL);
        api.registerEvent(TS3EventType.SERVER);
        api.addTS3Listeners(new TS3Listener() {
            @Override
            public void onTextMessage(TextMessageEvent e) {}

            @Override
            public void onServerEdit(ServerEditedEvent e) {}

            @Override
            public void onClientMoved(ClientMovedEvent e) {
                updateChannelList();
                updateChannels();
            }

            @Override
            public void onClientLeave(ClientLeaveEvent e) {
                updateChannelList();
                updateChannels();
            }

            @Override
            public void onClientJoin(ClientJoinEvent e) {
                updateChannelList();
                updateChannels();
            }

            @Override
            public void onChannelEdit(ChannelEditedEvent e) {}

            @Override
            public void onChannelDescriptionChanged(ChannelDescriptionEditedEvent e) {}

            @Override
            public void onChannelCreate(ChannelCreateEvent e) {}

            @Override
            public void onChannelDeleted(ChannelDeletedEvent e) {}

            @Override
            public void onChannelMoved(ChannelMovedEvent e) {}

            @Override
            public void onChannelPasswordChanged(ChannelPasswordChangedEvent e) {}

            @Override
            public void onPrivilegeKeyUsed(PrivilegeKeyUsedEvent e) {}
        });
    }
}