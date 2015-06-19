package edu.buffalo.cse.cse486586.groupmessenger2;

import java.io.Serializable;

/**
 * Created by archana on 3/8/15.
 */
public class MessagePacket implements Serializable {

    public String message;
    public String messageID;
    public int senderID;
    public double suggestedSeqNo;
    public int suggestionSender;
    //1-> actual message, 2-> proposed message, 3->agreed message
    public int type;

    //0-> undeliver 1-> deliver
    public boolean status;

    public int[] vector;

    public MessagePacket(String str, String id, int sender, int[] arr){
        message = str;
        messageID = id;
        senderID = sender;
        suggestedSeqNo = 0.0;
        suggestionSender = 0;
        type = 0;
        status = false;
        vector = arr;
    }

    public void setType(int type){
        this.type = type;
    }
    public void setSuggestedSeqNo(double sequence) {
        this.suggestedSeqNo = sequence;
    }
    public void setSuggestionSender(int i) {
        this.suggestionSender = i;
    }
    public void setStatus(boolean status){
        this.status = status;
    }
    public int getType(){
        return this.type;
    }
    public String getMessage(){
        return this.message;
    }
    public int getSenderID(){
        return this.senderID;
    }

    public double getSuggestedSeqNo(){
        return this.suggestedSeqNo;
    }

    public String getMessageID() {
        return this.messageID;
    }
    public boolean getStatus(){
        return this.status;
    }
}
