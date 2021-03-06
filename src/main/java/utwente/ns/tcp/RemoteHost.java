package utwente.ns.tcp;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * Created by Niels Overkamp on 18-Apr-17.
 */
@Data
@AllArgsConstructor
public class RemoteHost {
    private int address;
    private int port;

    public boolean equals(Object object) {
        return
                object instanceof RemoteHost
                        && ((RemoteHost) object).address == this.address
                        && ((RemoteHost) object).port == this.port;
    }

    public int hashCode() {
        return 15 * address + port;
    }


}
