package org.apache.isis.viewer.json.applib;

import static org.apache.isis.viewer.json.applib.JsonUtils.readJson;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;

import java.io.IOException;

import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.map.JsonMappingException;
import org.junit.Before;
import org.junit.Test;

public class JsonRepresentationTest_getRepresentation {

    private JsonRepresentation jsonRepresentation;

    @Before
    public void setUp() throws Exception {
        jsonRepresentation = new JsonRepresentation(readJson("map.json"));
    }
    
    @Test
    public void forMap() throws JsonParseException, JsonMappingException, IOException {
        JsonRepresentation mapRepresentation = jsonRepresentation.getRepresentation("aLink");
        assertThat(mapRepresentation.getString("rel"), is("someRel"));
        assertThat(mapRepresentation.isMap(), is(true));
    }

    @Test
    public void forNonExistent() throws JsonParseException, JsonMappingException, IOException {
        assertThat(jsonRepresentation.getRepresentation("doesNotExist"), is(nullValue()));
    }

    @Test
    public void forValue() throws JsonParseException, JsonMappingException, IOException {
        JsonRepresentation valueRepresentation = jsonRepresentation.getRepresentation("anInt");
        assertThat(valueRepresentation.isValue(), is(true));
    }

    @Test
    public void forList() throws JsonParseException, JsonMappingException, IOException {
        JsonRepresentation listRepresentation = jsonRepresentation.getRepresentation("aSubList");
        assertThat(listRepresentation.isArray(), is(true));
    }
    
    
}
