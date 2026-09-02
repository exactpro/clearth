/******************************************************************************
 * Copyright 2009-2019 Exactpro Systems Limited
 * https://www.exactpro.com
 * Build Software to Test Software
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/

package com.exactprosystems.clearth.woodpecker.exceptions;

import com.exactprosystems.clearth.woodpecker.initialdata.InitialData;

import java.nio.file.Path;
import java.util.Collection;
import java.util.List;

import static com.exactprosystems.clearth.utils.CollectionUtils.join;
import static com.exactprosystems.clearth.woodpecker.configuration.WoodpeckerConfigFile.START_FILE;
import static java.lang.String.format;

public class WoodpeckerFunctionsException extends WoodpeckerException
{
    public WoodpeckerFunctionsException(String message)
    {
        super(message);
    }

    public WoodpeckerFunctionsException(String message, Throwable cause)
    {
        super(message, cause);
    }
    

    public static WoodpeckerFunctionsException whenEvaluationFailed(Exception e, 
                                                                    String paramName, 
                                                                    String expression,
                                                                    Path templatePath)
    {
        return new WoodpeckerFunctionsException(format("Cannot generate parameter '%s' by formula [%s] in template '%s'.",
                paramName, expression, templatePath), e);
    }

    public static WoodpeckerFunctionsException whenRequiredParametersNotSpecified(String functionName,
                                                                                  List<String> paramsNames)
    {
        String message;
        if (paramsNames.size() == 1)
            message = format("Required for function '%s' parameter '%s' isn't specified.",
                    functionName, paramsNames.get(0));
        else
            message = format("Required for function '%s' parameters '%s' aren't specified.",
                    functionName, join(paramsNames));
        return new WoodpeckerFunctionsException(message);
    }

    public static WoodpeckerFunctionsException whenTableNotFound(String tableName,
                                                                 InitialData initialData)
    {
        Collection<String> available = initialData.getTableNames();
        return new WoodpeckerFunctionsException(format("Table '%s' isn't defined in %s. Available tables: %s.",
                tableName, START_FILE,
                available.isEmpty() ? "not found" : join(available)));
    }
    
    public static WoodpeckerFunctionsException whenQueryNotFound(String queryName, InitialData initialData)
    {
        Collection<String> available = initialData.getQueryNames();
        return new WoodpeckerFunctionsException(format("Query '%s' isn't defined in %s. Available queries: %s.", 
                queryName, START_FILE,
                available.isEmpty() ? "not found" : join(available)));
    }
    
    public static WoodpeckerFunctionsException whenCollectionNotFound(String collectionName, InitialData initialData)
    {
        Collection<String> available = initialData.getCollectionNames();
        return new WoodpeckerFunctionsException(format("Collection '%s' isn't defined in %s. Available collections: %s.", 
                collectionName, START_FILE,
                available.isEmpty() ? "not found" : join(available)));
    }
    
    public static WoodpeckerFunctionsException whenTryToAddNullToCollection(String collectionName)
    {
        return new WoodpeckerFunctionsException(format("Attempt to add null to collection '%s' is detected.", 
                collectionName));
    }
}
