package it.bologna.ausl.baborg.file.impl;

import it.bologna.ausl.baborg.file.FileRepository;
import it.bologna.ausl.baborg.file.next.NextFileManagerDateBase;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;


@Service
public class NextFileManagerImpl extends NextFileManagerDateBase {

    @Qualifier("nextFileFileRepository")
    @Autowired
    private FileRepository nextFileFileRepository;


    @Override
    public FileRepository getNextFileFileRepository() {
        return nextFileFileRepository;
    }
}
