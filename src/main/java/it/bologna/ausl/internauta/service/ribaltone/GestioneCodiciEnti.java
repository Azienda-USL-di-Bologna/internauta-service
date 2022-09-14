package it.bologna.ausl.internauta.service.ribaltone;

public class GestioneCodiciEnti {

    String[] whiteListUtenti;
    String[] whiteListCodiciEnti;

    public GestioneCodiciEnti() {
    }

    public GestioneCodiciEnti(String[] whiteListUtenti, String[] blackListCodiciEnti) {
        this.whiteListUtenti = whiteListUtenti;
        this.whiteListCodiciEnti = blackListCodiciEnti;
    }

    public String[] getWhiteListUtenti() {
        return whiteListUtenti;
    }

    public void setWhiteListUtenti(String[] whiteListUtenti) {
        this.whiteListUtenti = whiteListUtenti;
    }

    public String[] getBlackListCodiciEnti() {
        return whiteListCodiciEnti;
    }

    public void setBlackListCodiciEnti(String[] blackListCodiciEnti) {
        this.whiteListCodiciEnti = blackListCodiciEnti;
    }

}
