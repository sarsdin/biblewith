package com.example.androidclient.util

import android.util.Log
import com.google.gson.JsonArray
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

object Helper {



    /**
     * 날짜를 표시하는 딜리미터를 어느 인덱스의 요소에 나타낼지를 비교하여 찾아내고 표시하는 메소드
     * 표시될 요소를 찾아내면 속성값으로 is_dayChanged = 1 을 넣어줘서 뷰홀더에서 적용한다
     * 이때 != null && !isJsonNull 둘다비교처리해야함(뷰홀더) */
    fun 날짜표시기(it : JsonArray){
        val tagName = "[Helper:날짜표시기]"
        var 그다음날시작시간 : LocalDateTime? = null
//        var 날짜비교위한임시저장 :LocalDateTime? = null

        //일단 리스트의 0번 인덱스를 초기 비교기준으로 초기화해준다.
        if (it.size() == 0) {
            return
        }
        val 채팅시간 = LocalDateTime.parse(it.get(0).asJsonObject.get("create_date").asString, DateTimeFormatter.ofPattern("yyyy-MM-dd H:mm:ss"))
        val st그날시작날짜 = 채팅시간.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
        Log.e(tagName, "st그날시작날짜: $st그날시작날짜")
        val 그날시작시간 = LocalDateTime.parse("$st그날시작날짜 00:00:00", DateTimeFormatter.ofPattern("yyyy-MM-dd H:mm:ss"))
        Log.e(tagName, "그날시작시간: $그날시작시간")
        그다음날시작시간 = 그날시작시간.plusDays(1L)

        it.forEachIndexed { i, je ->
            val st_chat_date = je.asJsonObject.get("create_date").asString
            val chatDate = LocalDateTime.parse(st_chat_date, DateTimeFormatter.ofPattern("yyyy-MM-dd H:mm:ss"))
//                날짜비교위한임시저장 = chatDate

//            Log.e(tagName, "chatDate: $chatDate")

//            '그다음날인덱스의 시작시간'보다 현재 저장된 '그다음날시작시간'이 이전(적을경우)일경우 '그담날인덱스의 시작시간'으로 '그다음날시작시간'을 대체
            //반복문 마지막 인덱스시에에는 indexOutOfBound 가 뜨기때문에 조건을 리스트크기까지만 비교하는 걸로 맞춘다.
            val plus1indexOfChatDate = if (i+1 != it.size()) {
                LocalDateTime.parse(it.get(i+1).asJsonObject.get("create_date").asString, DateTimeFormatter.ofPattern("yyyy-MM-dd H:mm:ss"))
            } else { //마지막 인덱스일경우 현재 chatDate와 같은 데이터를 넣어서 다음 if(plus1indexOfChatDate.isAfter(그다음날시작시간))에서 반응안하게 하면됨(false되게).
                LocalDateTime.parse(it.get(i).asJsonObject.get("create_date").asString, DateTimeFormatter.ofPattern("yyyy-MM-dd H:mm:ss"))
            }
            if (plus1indexOfChatDate.isAfter(그다음날시작시간)) {
                val tmp_st = plus1indexOfChatDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
                그다음날시작시간 = LocalDateTime.parse("$tmp_st 00:00:00", DateTimeFormatter.ofPattern("yyyy-MM-dd H:mm:ss"))
//                그다음날시작시간 = plus1indexOfChatDate.truncatedTo(ChronoUnit.HOURS) //시간단위 이하는 0으로 날려버리고 반환됨- 근데 버그 있음.. 1시간 보다 작은 숫자가 반복적으로 나오면 시간에 2:00으로 표시됨.. 치명적!
//                Log.e(tagName, "그다음날시작시간 = 그담인덱스시간의 그날시작시간으로 건너띈 시작시간: $그다음날시작시간")
            }

//            Log.e(tagName, "chatDate: $chatDate, 그다음날시작시간: $그다음날시작시간 res: ${chatDate.isAfter(그다음날시작시간)}")
            if(chatDate.equals(그다음날시작시간) || chatDate.isAfter(그다음날시작시간)){
                //visible처리해줘야지
                val st그날시작날짜 = chatDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
                val 그날시작시간 = LocalDateTime.parse("$st그날시작날짜 00:00:00", DateTimeFormatter.ofPattern("yyyy-MM-dd H:mm:ss"))
                je.asJsonObject.addProperty("is_dayChanged",1) //!= null && !isJsonNull 둘다비교처리해야함(뷰홀더)
                //그리고, 현재 인덱스의 날짜의 시작시간+1day를 다음 dayChanged 레이아웃을 보여주는 비교기준으로 삼기위해 넣어줌
                //1 day를 기준으로 삼는 이유는 당연히 날짜 딜리미터(뷰)를 하루단위로 끊어서 보여줘야하기 때문에 1day 마다 비교를 해야함!
                그다음날시작시간 = 그날시작시간.plusDays(1L)
            }
        }
    }

}