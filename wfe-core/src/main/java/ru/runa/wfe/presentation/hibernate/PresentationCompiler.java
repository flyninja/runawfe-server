/*
 * This file is part of the RUNA WFE project.
 * 
 * This program is free software; you can redistribute it and/or 
 * modify it under the terms of the GNU Lesser General Public License 
 * as published by the Free Software Foundation; version 2.1 
 * of the License. 
 * 
 * This program is distributed in the hope that it will be useful, 
 * but WITHOUT ANY WARRANTY; without even the implied warranty of 
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the 
 * GNU Lesser General Public License for more details. 
 * 
 * You should have received a copy of the GNU Lesser General Public License 
 * along with this program; if not, write to the Free Software 
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA.
 */
package ru.runa.wfe.presentation.hibernate;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.hibernate.Query;

import ru.runa.wfe.presentation.BatchPresentation;
import ru.runa.wfe.presentation.BatchPresentationConsts;
import ru.runa.wfe.security.SecuredObjectType;

import com.google.common.collect.Lists;

/**
 * Creates {@link Query} to load data according to {@link BatchPresentation}.
 */
public class PresentationCompiler<T extends Object> implements IBatchPresentationCompiler<T> {

    /**
     * {@link BatchPresentation}, used to load data.
     */
    private final BatchPresentation batchPresentation;

    /**
     * Creates component to build loading data {@link Query}.
     * 
     * @param batchPresentation
     *            {@link BatchPresentation}, used to load data.
     */
    public PresentationCompiler(BatchPresentation batchPresentation) {
        this.batchPresentation = batchPresentation;
    }

    @SuppressWarnings("unchecked")
    @Override
    public final List<T> getBatch(CompilerParameters parameters) {
        return getBatchQuery(parameters).list();
    }

    @Override
    public final int getCount(CompilerParameters parameters) {
        return ((Number) getBatchQuery(new CompilerParameters(parameters, true)).uniqueResult()).intValue();
    }

    /**
     * Creates query to load data from database.
     * 
     * @param compilerParams
     *            {@link Query} creation parameters.
     * @return {@link Query} to load data from database.
     */
    protected Query getBatchQuery(CompilerParameters compilerParams) {
        HibernateCompilerQueryBuilder builder = new HibernateCompilerQueryBuilder(batchPresentation, compilerParams);
        Query query = builder.build();
        Map<String, QueryParameter> placeholders = builder.getPlaceholders();
        if (compilerParams.getExecutorIdsToCheckPermission() != null) {
            query.setParameterList("securedOwnersIds", compilerParams.getExecutorIdsToCheckPermission());
            query.setParameter("securedPermission", compilerParams.getPermission().getMask());
            List<Integer> typeOrdinals = Lists.newArrayList();
            for (SecuredObjectType type : compilerParams.getSecuredObjectTypes()) {
                typeOrdinals.add(type.ordinal());
            }
            query.setParameterList("securedTypes", typeOrdinals);
            placeholders.remove("securedOwnersIds");
            placeholders.remove("securedPermission");
            placeholders.remove("securedTypes");
        }
        if (compilerParams.hasOwners()) {
            query.setParameterList("ownersIds", compilerParams.getOwners());
            placeholders.remove("ownersIds");
        }
        if (compilerParams.isPagingEnabled() && batchPresentation.getRangeSize() != BatchPresentationConsts.RANGE_SIZE_UNLIMITED) {
            query.setFirstResult((batchPresentation.getPageNumber() - 1) * batchPresentation.getRangeSize());
            query.setMaxResults(batchPresentation.getRangeSize());
        }
        for (Iterator<Map.Entry<String, QueryParameter>> iter = placeholders.entrySet().iterator(); iter.hasNext();) {
            QueryParameter queryParameter = iter.next().getValue();
            query.setParameter(queryParameter.getName(), queryParameter.getValue());
        }
        return query;
    }
}
