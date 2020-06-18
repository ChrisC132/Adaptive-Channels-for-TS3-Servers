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
    private static List<Channel> channelList;
    private static Map<Integer, List<Integer>> channels;
    private static TS3Api api;

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
     * Updates channels.
     * All empty channels (except of one) of a channel group will be removed.
     * If a channel group is full a new channel in this group will be generated.
     */
    private static void updateChannels(){
        try{
            //Delete empty channels
            for (Map.Entry<Integer, List<Integer>> entry : channels.entrySet()) {
                for(int i = 1; i < entry.getValue().size()-1; i++) {
                    if(ChannelIsEmpty(entry.getValue().get(i))) {
                        System.out.println("Deleted " + entry.getValue().get(i));
                        api.deleteChannel(entry.getValue().get(i));
                        entry.getValue().remove(i);
                        i--;
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

                    int createID = api.createChannel(api.getChannelInfo(entry.getValue().get(entry.getValue().size()-1)).getName().substring(0,2)+ "\u200B" + api.getChannelInfo(entry.getValue().get(entry.getValue().size()-1)).getName().substring(2), properties);

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
        XMLReader xmlReader = new XMLReader();

        if(!xmlReader.validateXMLSchema()) {
            System.out.println("XML File is not valid");
            return;
        }

        channels = xmlReader.getChannels();

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