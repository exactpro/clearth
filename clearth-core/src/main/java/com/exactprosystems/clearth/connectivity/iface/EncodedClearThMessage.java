/******************************************************************************
 * Copyright 2009-2022 Exactpro Systems Limited
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

package com.exactprosystems.clearth.connectivity.iface;

import java.util.HashMap;

public class EncodedClearThMessage
{
    private final Object payload;
    private final ClearThMessageMetadata metadata;

    public EncodedClearThMessage(Object payload, ClearThMessageMetadata metadata)
    {
        this.payload = payload;
        this.metadata = metadata;
    }

    public EncodedClearThMessage(Object payload)
    {
        this(payload, null);
    }

    public Object getPayload()
    {
        return payload;
    }

    public ClearThMessageMetadata getMetadata()
    {
        return metadata;
    }
}
