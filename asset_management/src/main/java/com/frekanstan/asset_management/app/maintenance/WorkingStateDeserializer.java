package com.frekanstan.asset_management.app.maintenance;

import com.frekanstan.asset_management.data.acquisition.EWorkingState;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;

import java.lang.reflect.Type;

public class WorkingStateDeserializer implements JsonDeserializer<EWorkingState>
{
    @Override
    public EWorkingState deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext ctx)
            throws JsonParseException
    {
        int typeInt = json.getAsInt();
        return EWorkingState.findByAbbr(typeInt);
    }
}