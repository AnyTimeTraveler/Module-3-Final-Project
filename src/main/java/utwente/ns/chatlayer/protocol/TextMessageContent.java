package utwente.ns.chatlayer.protocol;

import lombok.NoArgsConstructor;

import java.io.UnsupportedEncodingException;
import java.security.Key;

/**
 * Created by Harindu Perera on 4/11/17.
 */
@NoArgsConstructor
public class TextMessageContent extends ChatMessageContent {

    String text = "";

    public TextMessageContent(String text) {
        this.text = text;
    }

    @Override
    public void setContent(Key key, String encData, byte[] ivBytes) {
        byte[] rawData = getDecryptedData(key, encData, ivBytes);
        this.text = new String(rawData);
    }

    @Override
    public byte[] getData() {
        try {
            return text.getBytes("utf8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            return new byte[0];
        }
    }

    public String toString() {
        return this.text;
    }

    @Override
    public String getType() {
        return ChatMessage.CONTENT_TYPE_TEXT;
    }
}
