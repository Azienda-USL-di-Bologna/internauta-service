package it.bologna.ausl.internauta.service.test.gedi;

import it.bologna.ausl.internauta.service.argo.utils.gd.SottoDocumentiUtils;
import it.bologna.ausl.internauta.service.configuration.utils.ReporitoryConnectionManager;
import it.bologna.ausl.minio.manager.MinIOWrapper;
import it.bologna.ausl.minio.manager.MinIOWrapperFileInfo;
import it.bologna.ausl.minio.manager.exceptions.MinIOWrapperException;
import java.util.List;
import java.util.Map;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

/**
 *
 * @author Salo
 */
//@RunWith(SpringRunner.class)
//@SpringBootTest
public class SottoDocumentiUtilsTest {

    @Autowired
    SottoDocumentiUtils sottoDocumentiUtils;

    @Autowired
    ReporitoryConnectionManager aziendeConnectionManager;

//    @Test
    public void testaReperirmentoCampiSottodocumenti() throws Exception {
        System.out.println("testaReperirmentoCampiSottodocumenti() ...");
        List<String> tableFieldsName = sottoDocumentiUtils.getTableFieldsName(2);
        System.out.println("FIELDS FOUND");
        for (String string : tableFieldsName) {
            System.out.println(string);
        }
    }

    //@Test
    public void testaCreazioneSottoDocumento() throws MinIOWrapperException, Exception {
        MinIOWrapper minIOWrapper = aziendeConnectionManager.getMinIOWrapper();
        MinIOWrapperFileInfo fileInfoByUuid = minIOWrapper.getFileInfoByUuid("d10a2fab-e624-4b97-98dc-7591fc861572");
        Map<String, Object> createSottoDocumento = sottoDocumentiUtils.createSottoDocumento(2, "xFi@L`d><bg;v]we,vL:", fileInfoByUuid, "SUDOCSBRUTTO");
        System.out.println(createSottoDocumento.toString());
    }

}
