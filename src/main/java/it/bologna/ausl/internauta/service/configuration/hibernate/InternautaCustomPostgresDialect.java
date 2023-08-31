package it.bologna.ausl.internauta.service.configuration.hibernate;

import com.vladmihalcea.hibernate.type.array.StringArrayType;
import com.vladmihalcea.hibernate.type.json.JsonNodeBinaryType;
import it.bologna.ausl.internauta.utils.jpa.tools.GenericArrayUserType;
import it.nextsw.common.dialect.CustomPostgresDialect;
import java.sql.Types;

/**
 *
 * @author gdm
 */
public class InternautaCustomPostgresDialect extends CustomPostgresDialect {

    public InternautaCustomPostgresDialect() {
        super();
//        this.registerColumnType(Types.ARRAY, GenericArrayUserType.class.getName());
//        registerHibernateType(Types.ARRAY, GenericArrayUserType.class.getName());
        
        this.registerColumnType(Types.OTHER, JsonNodeBinaryType.class.getName());
        registerHibernateType(Types.OTHER, JsonNodeBinaryType.class.getName());
        this.registerHibernateType(2003, StringArrayType.class.getName());
    }
}
