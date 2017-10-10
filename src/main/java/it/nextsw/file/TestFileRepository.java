package it.nextsw.file;

import it.nextsw.file.system.FileRepositoryFileSystem;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component
@Scope(ConfigurableBeanFactory.SCOPE_SINGLETON)
public class TestFileRepository extends FileRepositoryFileSystem {

    @Override
    public String getBaseUrl(){
        return "D:/FileRepo";
    }
}
