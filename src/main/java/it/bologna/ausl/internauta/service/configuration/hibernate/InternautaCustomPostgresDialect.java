/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.bologna.ausl.internauta.service.configuration.hibernate;

import it.bologna.ausl.internauta.utils.jpa.tools.GenericArrayUserType;
import it.nextsw.common.dialect.CustomPostgresDialect;
import java.sql.Types;

/**
 *
 * @author Salo
 */
public class InternautaCustomPostgresDialect extends CustomPostgresDialect {

    public InternautaCustomPostgresDialect() {
        super();
        this.registerColumnType(Types.ARRAY, GenericArrayUserType.class.getName());
        registerHibernateType(Types.ARRAY, GenericArrayUserType.class.getName());

    }
}
