package it.nextsw.file.next;


import it.nextsw.file.FileRepository;
import it.nextsw.file.exception.FileRepositoryException;
import it.nextsw.file.exception.NextFileException;
import it.nextsw.file.utils.NextFileUtils;
import org.apache.commons.io.IOUtils;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

/**
 * Un manager per la gestione degli oggetti con interfaccia {@link NextFile}
 * Utilizza un approccio con ramificazioni di directory in base alla {@link NextFile#getPath()}
 *
 * Created by f.longhitano on 30/07/2017.
 */
public abstract class NextFileManager {


    /**
     *
     * @return Il {@link FileRepository} dove salvare i file
     */
    public abstract FileRepository getNextFileFileRepository();

    /**
     * Legge il file
     * @param nextFile
     * @return l'inputStream del file
     * @throws NextFileException
     */
    public InputStream readFile(NextFile nextFile) throws NextFileException {
        try {
            return moveToDirectory(nextFile).readFile(nextFile.getFileName());

        }catch (Exception e){
            throw new NextFileException("Exception during read NextFile "+nextFile,e);
        }
    }

    /**
     * Per scrivere un file
     * @param nextFile
     * @return l'outputStream col quale si può scrivere sul file
     * @throws NextFileException
     */
    public OutputStream writeFile(NextFile nextFile) throws NextFileException {
        try {
            return moveToDirectory(nextFile).writeFile(nextFile.getFileName());
        }catch (Exception e){
            throw new NextFileException("Exception during write NextFile "+nextFile,e);
        }
    }

    /**
     * Persiste per la prima volta un file, crea quindi il file e scrive su di esso partendo dall'input stream dato come parametro
     * Valorizza il {@link NextFile#setFileName(String)}, {@link NextFile#setFileMimeType(String)}, {@link NextFile#setPath(String)}
     * Nel NextFile passato come parametro il {@link NextFile#getRealFileName()} deve tornare un valore valido
     * Non persiste nulla sul DB, ma solo su FS
     *
     * @param nextFile l'instanza di nextFile sul quale salvare le informazioni sul file salvato
     * @param inputStream l'inputstream contentente lo stream del file da scrivere
     * @throws NextFileException
     */
    public NextFile createFile(NextFile nextFile, InputStream inputStream) throws NextFileException {
        try {
            if (nextFile.getPath() == null)
                nextFile.setPath(createPathForNewFile(nextFile));
            FileRepository fileRepository = moveToDirectory(nextFile);
            String fileName = fileRepository.createFile(NextFileUtils.extractFileExtension(nextFile.getRealFileName()));
            OutputStream fileOutputStream = fileRepository.writeFile(fileName);
            IOUtils.copy(inputStream,fileOutputStream);
            IOUtils.closeQuietly(inputStream,fileOutputStream);
            nextFile.setFileName(fileName);
            nextFile.setFileMimeType(NextFileUtils.extractMimeTye(fileName));
            return nextFile;
        } catch (Exception e){
            throw new NextFileException("Exception during create NextFile "+nextFile,e);
        }
    }


    protected FileRepository moveToDirectory(NextFile nextFile) throws FileRepositoryException {
        List<String> parsedPath= parsePath(nextFile);
        FileRepository result=getNextFileFileRepository();
        for (String pathSegment : parsedPath)
            result=result.exploreDir(pathSegment);
        return result;
    }


    /**
     * Il metodo per splittare il path astrotto in una gerarchia di directory,
     * tenere in considerazione {@link NextFile#getPathDelimiter()}
     *
     * @param nextFile
     * @return una lista di stringhe con la gerarchia delle directory
     */
    protected abstract List<String> parsePath(NextFile nextFile);

    /**
     * Alla creazione di un nuovo {@link NextFile} si occupa di definire la stringa del path astratto
     * tenere in considerazione {@link NextFile#getPathDelimiter()}
     *
     * @param nextFile
     * @return la stringa del path
     */
    protected abstract String createPathForNewFile(NextFile nextFile);

}
