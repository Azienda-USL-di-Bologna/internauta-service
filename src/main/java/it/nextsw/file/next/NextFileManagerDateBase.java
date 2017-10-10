package it.nextsw.file.next;


import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

/**
 * Utilizza un approccio con ramificazioni di directory in base alla data di creazione del file
 *
 * Created by f.longhitano on 30/07/2017.
 */
public abstract class NextFileManagerDateBase extends NextFileManager{

    @Override
    protected List<String> parsePath(NextFile nextFile){
        List<String> result= new LinkedList<>();
        String[] split=nextFile.getPath().split(nextFile.getPathDelimiter());
        for(String pathSegment : split)
            result.add(pathSegment);

        return result;
    }

    @Override
    protected String createPathForNewFile(NextFile nextFile){
        DateFormat DF=new SimpleDateFormat("yyyy"+nextFile.getPathDelimiter()+"MM"+nextFile.getPathDelimiter()+"dd");
        return DF.format(new Date());
    }

}
