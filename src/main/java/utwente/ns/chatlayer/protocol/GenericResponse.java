package utwente.ns.chatlayer.protocol;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Created by Harindu Perera on 4/17/17.
 */
@NoArgsConstructor
@AllArgsConstructor
public class GenericResponse {

    @Getter
    private boolean success;
    @Getter
    private String message;

    public static GenericResponse successResponse() {
        return new GenericResponse(true, "");
    }

    public static GenericResponse failureResponse() {
        return new GenericResponse(false, "");
    }

    public static GenericResponse failureResponse(String message) {
        return new GenericResponse(false, message);
    }
}
