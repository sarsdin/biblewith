util.Http class에 구성된 api에서 Json/Gson object를 Response 객체로 활용하고 있다. 이것은 코드에서 어떤 데이터가 포함되는지와 흐름을 파악하기 어렵다.
명식적으로 필요한 Dto/Vo object를 생성하고, 그것을 사용하여 전체 api의 흐름을 처리하는 리펙토링을 진행하는 것이 핵심 목표이다.
필요한 직렬화/역직렬화 로직을 추가하여 Raw JsonObject/JsonArray/JsonElement 을 Dto/Vo로 대체하는 코드로 리펙토링해야한다.
특히, Gson 객체와 복잡하게 연계된 group 패키지 쪽의 코드들은 시간이 많이 걸려도 좋으니 차근차근 진행하도록 한다.
rtc 패키지는 기존 안드로이드의 개발방식이 아닌 Compose 방식으로 Ui가 개발되어 있다. 그리고, 웹소켓을 사용하고 gson의 직렬화/역직렬화의 빈도가 높으니 주의해서 리펙토링 하도록 한다.
