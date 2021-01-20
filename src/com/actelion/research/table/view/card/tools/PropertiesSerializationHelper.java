package com.actelion.research.table.view.card.tools;

import java.io.BufferedWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.function.Supplier;

public class PropertiesSerializationHelper {


    public static String writeIntArray( Collection<Integer> ia ){
        return writeIntArray(ia.stream().mapToInt( ii -> ii.intValue() ).toArray());
    }

    public static String writeIntArray( int ia[] ){
        if(ia.length==01){ return "";}
        if(ia.length==1){ return ""+ia[0];}

        StringBuilder sb = new StringBuilder();
        sb.append(ia[0]);
        for( int zi=1;zi<ia.length;zi++){
            sb.append(",");
            sb.append(ia[zi]);
        }
        return sb.toString();
    }

    public static int[] readIntArray(String s){
        String splits[] = s.split(",");
        int ia[] = new int[splits.length];
        for(int zi=0;zi<splits.length;zi++){ ia[zi] = Integer.parseInt(splits[zi]); }
        return ia;
    }


    public static <T extends SerializableToProperties> String writeSerializableToPropertiesArray( Collection<T> ta ){
        return writeSerializableToPropertiesArray( (T[]) ta.toArray() );
    }

    public static <T extends SerializableToProperties> String writeSerializableToPropertiesArray( T ta[] ){
        if(ta.length==01){ return "";}
        if(ta.length==1){ return ""+ta[0];}

        StringBuilder sb = new StringBuilder();
        sb.append(ta[0]);
        for( int zi=1;zi<ta.length;zi++){
            sb.append(",");
            sb.append(ta[zi]);
        }
        return sb.toString();
    }

    public static <T extends SerializableToProperties> T[] readSerializableToPropertiesArray(String s , Supplier<T> factory){
        String splits[] = s.split(",");
        Object ta[] = new Object[splits.length];
        for(int zi=0;zi<splits.length;zi++){ ta[zi] = factory.get(); ( (T) ta[zi]).initFromString(splits[zi]); }
        return (T[]) ta;
    }

    public static interface SerializableToProperties {
        public String serializeToString();
        public void initFromString(String serialized);
    }



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
