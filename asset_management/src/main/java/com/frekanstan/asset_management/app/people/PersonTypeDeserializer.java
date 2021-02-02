package com.frekanstan.asset_management.app.people;

import com.frekanstan.asset_management.data.people.EPersonType;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;

import java.lang.reflect.Type;

public class PersonTypeDeserializer implements JsonDeserializer<EPersonType>
{
    @Override
    public EPersonType deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext ctx)
            throws JsonParseException
    {
        int typeInt = json.getAsInt();
        return EPersonType.findByAbbr(typeInt);
    }
}