package com.zoho.takenote.utils

import android.graphics.Color

enum class ColorOption(val rgb: String) {
    RED("#F28B82"),
    ORANGE("#FABD04"),
    YELLOW("#FFF476"),
    MOSS_GREEN("#CDFF8F"),
    CYAN("#A7FEEB"),
    MINT("#CBF0F8"),
    BLUE("#AFCBFA"),
    PURPLE("#D7AEFC"),
    PINK("#FDCFE9"),
    BEIGE("#E6C9A9"),
    GREY("#E8EAED"),
    BLACK("#000000"),
    WHITE("#FFFFFF");


//    fun contrast(): ColorOption {
//        return if(this == BLACK){
//            GREY
//        } else{
//            BLACK
//        }
//    }

    companion object{

        fun getColorByValue(givenRgb: String): ColorOption{
            for(i in values()){
                if(i.rgb == givenRgb){
                    return i
                }
            }
            return BLUE
        }
    }

}