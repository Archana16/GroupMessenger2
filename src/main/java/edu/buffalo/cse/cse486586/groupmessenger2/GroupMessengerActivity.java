package edu.buffalo.cse.cse486586.groupmessenger2;

import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.app.Activity;
import android.telephony.TelephonyManager;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.SortedMap;
import java.util.Timer;
import java.util.TimerTask;
import java.util.TreeMap;
import java.util.concurrent.locks.ReentrantLock;

/**
 * GroupMessengerActivity is the main Activity for the assignment.
 *
 * @author stevko
 *
 */
public class GroupMessengerActivity extends Activity {

    static final String TAG = GroupMessengerActivity.class.getSimpleName();
    static final String[] REMOTE_PORT = {"11108","11112","11116","11120","11124"};
    static final int SERVER_PORT = 10000;
    static String myPort;
    static final String URL = "edu.buffalo.cse.cse486586.groupmessenger2.provider";

    public static Uri providerUri =null;
    public static int Sequence;
    public static int counter = 1;
    public static TreeMap<Double,MessagePacket> Holdback;
    public static QueueHolder queue;


    public static HashMap<String, ArrayList<MessagePacket>> suggestedlist;
    public static int icount = 0;
    private final ReentrantLock lock = new ReentrantLock();


    private static final int timeout_length = 4000;
    public static HashMap<Integer,Boolean> avdStatus ;
    public static HashMap<String, Timer> agreedTimers ;
    public static HashMap<String, Timer> proposalTimers ;


    public static int[] sequencevector = {0,0,0,0,0};
    public static TreeMap<Integer, MessagePacket> FIFOone;
    public static TreeMap<Integer, MessagePacket> FIFOtwo;
    public static TreeMap<Integer, MessagePacket> FIFOthree;
    public static TreeMap<Integer, MessagePacket> FIFOfour;
    public static TreeMap<Integer, MessagePacket> FIFOfive;

    public static ArrayList<TreeMap<Integer,MessagePacket>> FIFOQueues;
    public static TreeMap<Integer,MessagePacket> FIFOBuffer ;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group_messenger);


         /*
         * TODO: Use the TextView to display your messages. Though there is no grading component
         * on how you display the messages, if you implement it, it'll make your debugging easier.
         */
        TextView tv = (TextView) findViewById(R.id.textView1);
        tv.setMovementMethod(new ScrollingMovementMethod());

        /*
         * Registers OnPTestClickListener for "button1" in the layout, which is the "PTest" button.
         * OnPTestClickListener demonstrates how to access a ContentProvider.
         */
        findViewById(R.id.button1).setOnClickListener(
                new OnPTestClickListener(tv, getContentResolver()));

        TelephonyManager tel = (TelephonyManager) this.getSystemService(Context.TELEPHONY_SERVICE);
        String portStr = tel.getLine1Number().substring(tel.getLine1Number().length() - 4);
        myPort = String.valueOf((Integer.parseInt(portStr) * 2));
        /*
         * TODO: You need to register and implement an OnClickListener for the "Send" button.
         * In your implementation you need to get the message from the input box (EditText)
         * and send it to other AVDs.
         */

        providerUri = buildUri("content", URL);
        Holdback = new TreeMap<Double,MessagePacket>();
        queue = new QueueHolder(Holdback);

        suggestedlist = new HashMap<String,ArrayList<MessagePacket>>();
        avdStatus = new HashMap<Integer, Boolean>();
        for(String port : REMOTE_PORT){
            avdStatus.put(Integer.parseInt(port), true);
        }

        agreedTimers = new HashMap<String,Timer>();
        proposalTimers = new HashMap<String,Timer>();

        FIFOone = new TreeMap<Integer,MessagePacket>();
        FIFOtwo = new TreeMap<Integer,MessagePacket>();
        FIFOthree = new TreeMap<Integer,MessagePacket>();
        FIFOfour = new TreeMap<Integer,MessagePacket>();
        FIFOfive = new TreeMap<Integer,MessagePacket>();

        FIFOQueues =new ArrayList<TreeMap<Integer,MessagePacket>>();
        FIFOQueues.add(FIFOone);
        FIFOQueues.add(FIFOtwo);
        FIFOQueues.add(FIFOthree);
        FIFOQueues.add(FIFOfour);
        FIFOQueues.add(FIFOfive);
        FIFOBuffer = new TreeMap<Integer, MessagePacket>();


        try {
            ServerSocket serverSocket = new ServerSocket(SERVER_PORT);
            new ServerTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, serverSocket);
        } catch (IOException e) {
            Log.e(TAG, "Can't create a ServerSocket");
            return;
        }

        final EditText editText = (EditText) findViewById(R.id.editText1);

        findViewById(R.id.button4).setOnClickListener(
                new Button.OnClickListener(){
                    public void onClick(View v){

                String msg = editText.getText().toString() + "\n";
                        String msgToSend = msg.trim();
                        String messageId=String.valueOf(counter)+"-"+myPort;
                        sequencevector[getIndexforPort(Integer.parseInt(myPort))] = counter;
                        MessagePacket message = new MessagePacket(msgToSend,messageId,Integer.parseInt(myPort),sequencevector);
                        message.setType(1);
                        counter = counter+1;

                editText.setText(""); // This is one way to reset the input box.
                TextView localTextView = (TextView) findViewById(R.id.local_text_display);
                localTextView.append("\t" + msg); // This is one way to display a string.
                TextView remoteTextView = (TextView) findViewById(R.id.remote_text_display);
                remoteTextView.append("\n");
                new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, message,null);
                    }
                }
        );
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.activity_group_messenger, menu);
        return true;
    }

    private int getIndexforPort(int port){
        switch(port){
            case 11108 : return 0;
            case 11112 : return 1;
            case 11116 : return 2;
            case 11120 : return 3;
            case 11124 : return 4;
        }
        return -1;
    }


    private Uri buildUri(String scheme, String authority) {
        Uri.Builder uriBuilder = new Uri.Builder();
        uriBuilder.authority(authority);
        uriBuilder.scheme(scheme);
        return uriBuilder.build();
    }

    private class QueueHolder{
        private TreeMap<Double,MessagePacket> syncedQueue;
        private ReentrantLock lock;

        QueueHolder(TreeMap<Double,MessagePacket> map ){

            this.syncedQueue = map;
            lock = new ReentrantLock();
        }

        public synchronized int getSize(){
            lock.lock();
            int i =this.syncedQueue.size();
            lock.unlock();
            return  i;
        }
        public synchronized int getlastKey(){
            int i = -1;
            lock.lock();
            if(!this.syncedQueue.isEmpty()){
               i =this.syncedQueue.lastKey().intValue();
            }
            lock.unlock();
            return i;
        }
        public synchronized void remove(double key){
            lock.lock();
            if(this.syncedQueue.containsKey(key))
            this.syncedQueue.remove(key);
            lock.unlock();
        }
        public synchronized void add(double key, MessagePacket obj){
            lock.lock();
            this.syncedQueue.put(key,obj);
            lock.unlock();
        }

        public synchronized  void update(MessagePacket obj){

            lock.lock();
            for (Map.Entry<Double, MessagePacket> entry : this.syncedQueue.entrySet()) {
                MessagePacket m = entry.getValue();
                Double seq = entry.getKey();
                if (m.getMessageID().equals(obj.getMessageID())) {
                    this.syncedQueue.remove(seq);
                    obj.setStatus(true);
                    this.syncedQueue.put(obj.getSuggestedSeqNo(), obj);
                    break;
                }

            }
            lock.unlock();
            return;

        }

        public synchronized MessagePacket deliver(){
            MessagePacket m = null;
            lock.lock();
            if (!this.syncedQueue.isEmpty() && this.syncedQueue.get(this.syncedQueue.firstKey()).getStatus()) {
               m = this.syncedQueue.remove(this.syncedQueue.firstKey());

            }
            lock.unlock();
            return m;

        }

        public synchronized void removebyId(String id){
            lock.lock();
            for (Map.Entry<Double, MessagePacket> entry : this.syncedQueue.entrySet()) {
                MessagePacket m = entry.getValue();
                Double seq = entry.getKey();
                if (m.getMessageID().equals(id)){
                    this.syncedQueue.remove(seq);
                    break;
                }

            }
            lock.unlock();

        }


    }


    private class ClientTask extends AsyncTask<MessagePacket, Void, Void> {
        @Override
        protected Void doInBackground(MessagePacket... params) {
            MessagePacket message = params[0];
            int type = message.getType();
            if(type == 1){
                Timer proposal = new Timer();
                HandleTimeout  proposalTimerTask = new HandleTimeout(message.getMessageID(),"proposal");
                proposal.schedule(proposalTimerTask,7000);
                proposalTimers.put(message.getMessageID(),proposal);
            }
            try {

                if(type ==2){

                    if(avdStatus.get(message.getSenderID())) {
                        Socket socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),
                                message.getSenderID());

                        ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
                        out.writeObject(message);
                        out.close();
                        socket.close();
                    }
                }else {

                    for(String remotePort : REMOTE_PORT) {
                        if(avdStatus.get(Integer.parseInt(remotePort))) {

                            try {
                                Socket socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),
                                        Integer.parseInt(remotePort));

                                ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
                                out.writeObject(message);

                                out.close();
                                socket.close();
                            }catch(IOException e){
                                Log.e("client exception ",e.getMessage());
                                avdStatus.put(Integer.parseInt(remotePort), false);
                            }
                        }
                    }

                }
            }catch (UnknownHostException e) {
                Log.e(TAG, "ClientTask UnknownHostException");
            } catch (IOException e) {
                Log.e(TAG, "client exception unicast "+e.getMessage());
                avdStatus.put(message.getSenderID(),false);
            }

            return null;
        }
    }


    private class ServerTask extends AsyncTask<ServerSocket, MessagePacket, Void> {

        @Override
        protected Void doInBackground(ServerSocket... sockets) {

            ServerSocket serverSocket = sockets[0];

            try {

                //serverSocket.setSoTimeout(timeout_length);
                while(true) {
                    Socket listener = serverSocket.accept();
                    ObjectInputStream in = new ObjectInputStream(listener.getInputStream());
                    MessagePacket Incoming = (MessagePacket) in.readObject();
                    synchronized(this){
                    if (Incoming != null) {
                        try {
                            if (Incoming.getType() == 1) {

                                Timer agree = new Timer();
                                HandleTimeout agreedTimerTask = new HandleTimeout(Incoming.getMessageID(), "agreement");
                                agree.schedule(agreedTimerTask,10000);
                                agreedTimers.put(Incoming.getMessageID(), agree);

                                ArrayList<MessagePacket> accept = new ArrayList<MessagePacket>();

                                if (Incoming.getSenderID() != Integer.parseInt(myPort)) {
                                    Log.e("first", "recieved : " + Incoming.getMessageID() + " from :" + Incoming.getSenderID());
                                    int index = getIndexforPort(Incoming.getSenderID());
                                    int s = Incoming.vector[index];
                                    int r = sequencevector[index];
                                    FIFOBuffer = FIFOQueues.get(index);

                                    if (s < r + 1) {
                                        //reject
                                    } else if (s > r + 1) {
                                        // buffer in queue
                                        FIFOBuffer.put(s, Incoming);
                                    } else if (s == r + 1) {
                                        //accept
                                        accept.add(Incoming);
                                        sequencevector[index]++;

                                        while (!FIFOBuffer.isEmpty()) {
                                            int key = FIFOBuffer.firstKey();
                                            MessagePacket m = FIFOBuffer.get(key);
                                            Log.e("buffred", m.getSuggestedSeqNo() + "");
                                            s = m.vector[index];
                                            r = sequencevector[index];
                                            if (s == r + 1) {
                                                accept.add(m);
                                                sequencevector[index]++;
                                                FIFOBuffer.remove(key);
                                            } else {
                                                break;
                                            }

                                        }
                                    }
                                } else {
                                    Log.e("first", "recieved : " + Incoming.getMessageID() + " from myself");
                                    accept.add(Incoming);
                                }
                                for (MessagePacket acceptMessage : accept) {

                                    //if (Holdback.size() == 0) {
                                    if (queue.getSize() == 0) {
                                        Sequence = Sequence + 1;
                                    } else {
                                        //int highest = Holdback.lastKey().intValue();
                                        int highest = queue.getlastKey();
                                        Sequence = Math.max(counter, highest) + 1;
                                    }
                                    Double seq = (Double) Double.parseDouble(Sequence + "." + myPort);
                                    acceptMessage.setSuggestedSeqNo(seq.doubleValue());
                                    acceptMessage.setSuggestionSender(Integer.parseInt(myPort));

                                    //add in Holdback
                                    lock.lock();

                                    //Holdback.put(acceptMessage.getSuggestedSeqNo(), acceptMessage);
                                    queue.add(acceptMessage.getSuggestedSeqNo(),acceptMessage);
                                    lock.unlock();


                                    acceptMessage.setType(2);

                                    //multicast
                                    ServerHelper obj = new ServerHelper(acceptMessage);
                                    obj.multicast();
                                }
                            } else if (Incoming.getType() == 2) {

                                Log.e("second", "recieved proposal from: " + Incoming.suggestionSender + " for" + Incoming.getMessageID());
                                ArrayList<MessagePacket> list = null;
                                if (!suggestedlist.containsKey(Incoming.getMessageID())) {
                                    ArrayList<MessagePacket> newlist = new ArrayList<MessagePacket>();
                                    suggestedlist.put(Incoming.getMessageID(), newlist);
                                }
                                list = suggestedlist.get(Incoming.getMessageID());
                                list.add(Incoming);
                                if (list.size() == 5) {
                                    Log.e("second", "recieved all proposals for" + Incoming.getMessageID());

                                    Timer t = proposalTimers.get(Incoming.getMessageID());
                                    if (t != null) {

                                        t.cancel();
                                        t = null;
                                        Log.e("timer", "canceled proposal time for" + Incoming.getMessageID());
                                    }
                                    Collections.sort(list, new ListMessageSorter());
                                    MessagePacket Broadcast = list.get(0);
                                    Broadcast.setStatus(true);
                                    Broadcast.setType(3);
                                    ServerHelper obj = new ServerHelper(Broadcast);
                                    obj.multicast();
                                    suggestedlist.remove(Incoming.getMessageID());
                                }
                            } else if (Incoming.getType() == 3) {
                                Timer t = agreedTimers.get(Incoming.getMessageID());
                                if (t != null) {
                                    t.cancel();
                                    t = null;
                                    Log.e("timer", "canceled agreement time for" + Incoming.getMessageID());
                                }
                                queue.update(Incoming);
                                MessagePacket m1;
                                while(( m1= queue.deliver())!= null){
                                    publishProgress(m1);
                                }
                             /*   for (Map.Entry<Double, MessagePacket> entry : Holdback.entrySet()) {
                                    MessagePacket m = entry.getValue();
                                    Double seq = entry.getKey();
                                    if (m.getMessageID().equals(Incoming.getMessageID())) {
                                        lock.lock();
                                        Holdback.remove(seq);
                                        Incoming.setStatus(true);
                                        Holdback.put(Incoming.getSuggestedSeqNo(), Incoming);
                                        lock.unlock();
                                        break;
                                    }

                                }*/
                               /* while (Holdback.get(Holdback.firstKey()).getStatus()) {
                                    publishProgress(Holdback.get(Holdback.firstKey()));
                                    Holdback.remove(Holdback.firstKey());
                                    if (Holdback.size() == 0) {
                                        break;
                                    }
                                }*/

                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }

                    }
                }
                }
            }catch(ClassNotFoundException e){
                e.printStackTrace();
            }catch(IOException e){

            }


            return null;
        }

        protected void onProgressUpdate(MessagePacket...packet) {
            /*
             * The following code displays what is received in doInBackground().
             */
            MessagePacket packetReceived = packet[0];

            String recieved = packetReceived.getMessage();
            TextView remoteTextView = (TextView) findViewById(R.id.remote_text_display);
            remoteTextView.append(recieved + "\t\n");
            TextView localTextView = (TextView) findViewById(R.id.local_text_display);
            localTextView.append("\n");


                    ContentValues keyValueToInsert = new ContentValues();
                    keyValueToInsert.put("key",String.valueOf(icount));
                    keyValueToInsert.put("value",recieved);
            icount++;

                    getContentResolver().insert(
                            providerUri, keyValueToInsert);
            return;
        }
    }

    private class ListMessageSorter implements Comparator<MessagePacket>
    {
        @Override
        public int compare(MessagePacket x,MessagePacket y)
        {
            //sort in non-increasing sequence number
            if(x.suggestedSeqNo < y.suggestedSeqNo)
            {
                return 1;
            }else
                return -1;
        }
    }
    /*private class QueueMessageSorter implements Comparator<MessagePacket>
    {
        @Override
        public int compare(MessagePacket x,MessagePacket y)
        {
            //sort in increasing sequence number
            if(x.suggestedSeqNo > y.suggestedSeqNo)
            {
                return 1;
            }
            else if(x.suggestedSeqNo < y.suggestedSeqNo){
                return -1;
            }
            else{
                //sequence number is same then return smallest sender
                if(x.senderID > y.senderID)
                    return 1;
                else
                    return 0;
            }
        }
    }*/

    private class ServerHelper
    {
        MessagePacket sendmessage;
        ServerHelper(MessagePacket msg)
        {
            this.sendmessage=msg;
        }
        public void multicast()
        {
            new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, this.sendmessage,null);
        }
    }

    private class HandleTimeout extends TimerTask {

        String messageId;
        String type;

        public HandleTimeout(String param1, String param2) {
            this.messageId = param1;
            this.type = param2;
        }

        @Override
        public void run() {
            if(type.equals("agree")){
                //delete the message waiting for the agreed message
                Log.e("timer-agree","time out for : "+this.messageId);
                Log.e("timer-agree","action - removed message: "+this.messageId);

                queue.removebyId(messageId);
                /*lock.lock();
                    Holdback.remove(messageId);
                lock.unlock();*/

            }
            if(type.equals("proposal")){
                Log.e("timer-proposal","time out for : "+this.messageId);
                ArrayList<MessagePacket> list= suggestedlist.get(messageId);
                if(list !=null){

                    Collections.sort(list, new ListMessageSorter());
                    MessagePacket Broadcast = list.get(0);
                    Log.e("timer-proposal","action - selected"+Broadcast.getSuggestedSeqNo());
                    Broadcast.setStatus(true);
                    Broadcast.setType(3);
                    ServerHelper obj = new ServerHelper(Broadcast);
                    obj.multicast();
                    suggestedlist.remove(messageId);

                }

            }


        }

    }



}
