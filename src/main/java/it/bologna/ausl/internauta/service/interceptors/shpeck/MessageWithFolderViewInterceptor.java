package it.bologna.ausl.internauta.service.interceptors.shpeck;

import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.dsl.BooleanExpression;
import it.bologna.ausl.internauta.service.authorization.AuthenticatedSessionData;
import it.bologna.ausl.internauta.service.interceptors.InternautaBaseInterceptor;
import it.bologna.ausl.internauta.service.utils.InternautaConstants.AdditionalData;
import it.bologna.ausl.model.entities.shpeck.Folder.FolderType;
import it.bologna.ausl.model.entities.shpeck.views.MessageWithFolderView;
import it.bologna.ausl.model.entities.shpeck.views.QMessageWithFolderView;
import it.nextsw.common.data.annotations.NextSdrInterceptor;
import it.nextsw.common.interceptors.exceptions.AbortLoadInterceptorException;
import java.util.List;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 *
 * @author gusgus
 */
@Component
@NextSdrInterceptor(name = "messagewithfolderview-interceptor")
public class MessageWithFolderViewInterceptor extends InternautaBaseInterceptor {

    private static final Logger LOGGER = LoggerFactory.getLogger(MessageWithFolderViewInterceptor.class);

    @Autowired
    MessageInterceptorUtils messageInterceptorUtils;
    
    @Override
    public Class getTargetEntityClass() {
        return MessageWithFolderView.class;
    }

    @Override
    public Predicate beforeSelectQueryInterceptor(Predicate initialPredicate, Map<String, String> additionalData, HttpServletRequest request, boolean mainEntity, Class projectionClass) throws AbortLoadInterceptorException {
        AuthenticatedSessionData authenticatedUserProperties = super.getAuthenticatedUserProperties();

        initialPredicate = messageInterceptorUtils.messageInPecWithPermission(authenticatedUserProperties, MessageWithFolderView.class).and(initialPredicate);

        List<AdditionalData.OperationsRequested> operationsRequested = AdditionalData.getOperationRequested(AdditionalData.Keys.OperationRequested, additionalData);
        if (operationsRequested != null && !operationsRequested.isEmpty()) {
            for (AdditionalData.OperationsRequested operationRequested : operationsRequested) {
                switch (operationRequested) {
                    case FiltraSuTuttiFolderTranneTrash:
                        QMessageWithFolderView qMessageWithFolderView = QMessageWithFolderView.messageWithFolderView;
                        BooleanExpression filtro = qMessageWithFolderView.idFolder.type.ne(FolderType.TRASH.toString());
                        initialPredicate = filtro.and(initialPredicate);
                        break;
                }
            }
        }
        return initialPredicate;
    }
}
