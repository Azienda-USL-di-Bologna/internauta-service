package it.nextsw.file.next;

import java.util.Date;

/**
 * Interfaccia da far implementare agli oggetti che rappresentano un'astrazione di File
 * Created by f.longhitano on 28/07/2017.
 */
public interface NextFile {


    /**
     *
     * @return Il nome del file così come salvato su sorgente
     */
    public String getFileName();

    public void setFileName(String fileName);

    /**
     *
     * @return Il nome del file per l'utilizzo utente, viene utilizzato solo per estrarre l'estensione
     */
    public String getRealFileName();

    public void setRealFileName(String realFileName);

    /**
     *
     * @return il mimeType del file
     */
    public String getFileMimeType();

    public void setFileMimeType(String mimeType);

    /**
     *
     * @return il path astratto del file con le directori divise dalla string {@link #getPathDelimiter()}
     */
    public String getPath();

    public void setPath(String path);


    /**
     *
     * @return la stringa che mi permette di divide {@link #getPath()} nella gerarchia di directory
     */
    public String getPathDelimiter();
}
