import android.graphics.Bitmap
import okhttp3.MediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.InputStream
import java.security.AccessController.getContext

open class Test(var age2: Int) {
    var age3: Int = 22

    //filepath는 String 변수로 갤러리에서 이미지를 가져올 때 photoUri.getPath()를 통해 받아온다.
//    fun dd() {
//        File file = new File(filepath);
//        InputStream inputStream = null;
//        try {
//            inputStream = getContext().getContentResolver().openInputStream(photoUri);
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//        Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
//        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
//        bitmap.compress(Bitmap.CompressFormat.JPEG, 20, byteArrayOutputStream)
//        RequestBody requestBody = RequestBody.create(MediaType.parse("image/jpg"), byteArrayOutputStream.toByteArray())
//        MultipartBody.Part uploadFile = MultipartBody.Part.createFormData("postImg", file.getName(), requestBody)
//    }

}