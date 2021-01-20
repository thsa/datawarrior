package com.actelion.research.datawarrior.task.view.cards;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class MyProperties {

    private Properties p;

    public MyProperties(Properties p) {
        this.p = p;
    }

    public Properties getPropertiesObject() { return p;}

    public String getProperty(String name){ return p.getProperty(name); }

    public String getString(String name){ return p.getProperty(name); }

    public boolean getBool(String name){ return Boolean.parseBoolean(p.getProperty(name)); }

    public Integer getInt(String name){ return (p.getProperty(name).equals("NULL") ) ? null: Integer.parseInt(p.getProperty(name)); }

    public double getDouble(String name){ return Double.parseDouble(p.getProperty(name)); }


    public static final String STRING_LIST_SEPARATOR_STRING = "<;>";
    public static final String EMPTY_STRING_LIST_STRING = "<<_empty_list_>>";

    /**
     *
     * @param strings
     * @return string list serialized to string
     * @throws Exception if list conains substrings that make it unserializable
     */
    public static String stringListToString(List<String> strings) throws Exception{

        if( strings.stream().anyMatch(  si -> si.contains(STRING_LIST_SEPARATOR_STRING)) ){ throw new Exception("Cannot serialize, found separator string.."); }
        if( strings.stream().anyMatch(  si -> si.contains(EMPTY_STRING_LIST_STRING)) ){ throw new Exception("Cannot serialize, found empty list string.."); }

        if(strings.size()==0) {
            return EMPTY_STRING_LIST_STRING;
        }

        StringBuilder sb = new StringBuilder();
        for(int zi=0;zi<strings.size();zi++) {
            sb.append( strings.get(zi) );
            if(zi!=strings.size()-1){ sb.append(STRING_LIST_SEPARATOR_STRING); }
        }

        return sb.toString();
    }

    /**
     * Deserializes a serialized string list from a string
     *
     * @param strings
     * @return the deserialized list
     */
    public static List<String> stringListToString_inverse(String strings) {
        if( strings.equals(EMPTY_STRING_LIST_STRING) ) {
            return new ArrayList<>();
        }

        String splits[] = strings.split(STRING_LIST_SEPARATOR_STRING);
        List<String> list = new ArrayList<>();

        for(int zi=0;zi<splits.length;zi++){list.add(splits[zi]);}

        return list;
    }

}
