package it.bologna.ausl.organigramma.file;

import it.bologna.ausl.baborg.file.utils.NameGenerationUtil;

/**
 * Created by user on 27/06/2017.
 */
public class FileRepositoryBase {



    public String createFile(){
        String generatedName= NameGenerationUtil.generateName();


        return generatedName;
    }


    private String getBaseUrl(){
        return "D:/FileRepo";
    }

}
