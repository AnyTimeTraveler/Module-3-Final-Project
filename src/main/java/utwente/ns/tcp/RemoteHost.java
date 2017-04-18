package utwente.ns.tcp;

import lombok.Data;

/**
 * Created by Niels Overkamp on 18-Apr-17.
 */
@Data
public class RemoteHost{
    int address;
    int port;

    public boolean equals(Object object){
        return
                object instanceof RemoteHost
                && ((RemoteHost) object).address == this.address
                && ((RemoteHost) object).port == this.port;
    }
}
