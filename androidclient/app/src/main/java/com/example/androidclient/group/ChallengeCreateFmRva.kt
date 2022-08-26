package com.example.androidclient.group

import android.graphics.*
import android.graphics.drawable.BitmapDrawable
import android.os.Build
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.androidclient.R
import com.example.androidclient.bible.BibleVm
import com.example.androidclient.databinding.ChallengeCreateFmVhBinding
import com.example.androidclient.moreinfo.MyNoteRvaInner
import com.google.android.material.button.MaterialButton
import com.google.gson.JsonArray
import com.google.gson.JsonObject


class ChallengeCreateFmRva(val groupVm: GroupVm, val bibleVm: BibleVm, val challengeCreateFm: ChallengeCreateFm)
    : RecyclerView.Adapter<ChallengeCreateFmRva.ChallengeCreateFmVh>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChallengeCreateFmRva.ChallengeCreateFmVh {
        return ChallengeCreateFmVh(ChallengeCreateFmVhBinding.inflate(LayoutInflater.from(parent.context), parent,false))
    }

    override fun onBindViewHolder(holder: ChallengeCreateFmVh, position: Int) {
        holder.bind(groupVm.createList[position] as JsonObject)
    }

    override fun getItemCount(): Int {
        return groupVm.createList.size()
    }

    override fun onViewAttachedToWindow(holder: ChallengeCreateFmVh) {
        holder.setIsRecyclable(false) //홀더뷰 재사용 안함으로 설정하여 홀더의 절 ui들이 독립적으로 작동하게 함
        super.onViewAttachedToWindow(holder)
    }

    inner class ChallengeCreateFmVh(var binding: ChallengeCreateFmVhBinding) : RecyclerView.ViewHolder(binding.root) {
        lateinit var mItem : JsonObject
        var rv: RecyclerView? =null

        init {
        }

        //mItem -- 모임목록가져오기()
        fun bind(mItem: JsonObject) {
            this.mItem = mItem;
            binding.selectedBibleBt.text = mItem.get("book_name").asString
//                groupVm.selectedCreateList

            //현재 홀더의 체크유무에 따른 style을 적용해 주고 버튼 클릭때 다시 갱신해주는 방식!
            if (mItem.get("is_selected").asBoolean) {
                binding.selectedBibleBt.setIconTintResource(R.color.color_transparent)
                binding.selectedBibleBt.iconTintMode = PorterDuff.Mode.ADD
                var index = 99
                groupVm.selectedCreateList.forEachIndexed { i, jo ->
                    if(jo.asJsonObject.get("book").asInt == mItem.get("book").asInt){
                        index = i+1
                    }
                }
                숫자이미지넣기(binding.selectedBibleBt, index) //챌린지 만들때 선택한 성경책에 번호의 순서를 바꿔줘야할때 쓰임

            } else {
                binding.selectedBibleBt.setIconTintResource(R.color.colorLike_gray)
                binding.selectedBibleBt.iconTintMode = PorterDuff.Mode.SRC_IN
                binding.selectedBibleBt.setIconResource(R.drawable.ic_baseline_check_circle_24)
            }


            //각 성경책 선택시 선택리스트에 추가하고 번호순서로 숫자 표시하기
            binding.selectedBibleBt.setOnClickListener {
                버튼클릭동작()
                notifyDataSetChanged()
            }




        }

        fun 버튼클릭동작(){
            //선택유무를 체크해서 토글처리해준다.
            if (mItem.get("is_selected").asBoolean) {
                mItem.addProperty("is_selected", false)
                binding.selectedBibleBt.setIconTintResource(R.color.colorLike_gray)
                binding.selectedBibleBt.iconTintMode = PorterDuff.Mode.SRC_IN
                binding.selectedBibleBt.setIconResource(R.drawable.ic_baseline_check_circle_24)
//                    groupVm.selectedCreateList.forEach { it2 ->
//                        if(it2.asJsonObject.get("book").asInt == mItem.get("book").asInt){
//                            groupVm.selectedCreateList.remove(it2) //현재 클릭한 홀더의 정보를 선택리스트에서 제거함(선택제거)
//                        }
//                    }
                val tmpL = groupVm.selectedCreateList.iterator()
                while(tmpL.hasNext()){
                    val item = tmpL.next()
                    if(item.asJsonObject.get("book").asInt == mItem.get("book").asInt){
                        //iterator remove의 주의점: 컬렉션에서의 remove는 인덱스를 인수로 주고 그인덱스에 해당하는 요소를 제거하는 메소드이다.
                        //하지만, iterator의 remove 메소드는 현재 next()로 반환된 마지막 요소를 이미 알고 있기 때문에 인덱스를 쓰지않고,
                        //그 알고 있는 요소를 제거하라는 심플한 명령에 가깝다.
                        //고로, [tmpL == iterator]는  [마지막요소 == next()]를  [제거하라 == tmpL.remove()]
                        tmpL.remove() //현재 클릭한 홀더의 정보를 선택리스트에서 제거함(선택제거)
                    }
                }

            } else {
                mItem.addProperty("is_selected", true)
                groupVm.selectedCreateList.add(mItem.deepCopy()) //현재 선택 홀더를 복사하여 선택리스트에 넣음(선택처리)
                binding.selectedBibleBt.setIconTintResource(R.color.color_transparent)
                binding.selectedBibleBt.iconTintMode = PorterDuff.Mode.ADD
                var index = 99
                groupVm.selectedCreateList.forEachIndexed { i, jo ->
                    if(jo.asJsonObject.get("book").asInt == mItem.get("book").asInt){
                        index = i+1
                    }
                }
                숫자이미지넣기(binding.selectedBibleBt, index) //챌린지 만들때 선택한 성경책에 번호의 순서를 바꿔줘야할때 쓰임
            }
            groupVm.liveSelectedCreateList.value = groupVm.selectedCreateList
        }

        fun 숫자이미지넣기(buttonView: MaterialButton, position: Int){
            val fontSize = 40 //set your font size
//            val baseImage = BitmapFactory.decodeResource(challengeCreateFm.requireContext().resources, R.drawable.ic_baseline_circle_24) //load the hexagon image
//            val baseImage = AppCompatResources.getDrawable(challengeCreateFm.requireContext(), R.drawable.ic_baseline_circle_24)!!
//                .toBitmap(50, 50)
//            val image = baseImage.copy(Bitmap.Config.ARGB_8888,true) //create mutable bitmap for canvas. see: http://stackoverflow.com/a/13119762/1896516
            val image = 백터이미지비트맵으로변환()

            //the "paint" to use when drawing the text
            val paint = Paint() //see here for all paint options: https://developer.android.com/reference/android/graphics/Paint.html
            paint.setTextSize(fontSize.toFloat()) //sets your desired font size
            paint.setColor(Color.WHITE) //set the desired color of the text
            paint.setTextAlign(Paint.Align.CENTER) //set the alignment of the text

            val canvas = Canvas(image) //create a canvas from the bitmap
//            canvas.drawColor(Color.GREEN)
            canvas.drawText(position.toString(), (image.width / 2).toFloat(),
                (image.height / 2 + fontSize / 4).toFloat()+3.5f, paint) //draw the number onto the hexagon
//            val iv: ImageView = findViewById(com.example.androidclient.R.id.iv) as ImageView
//            buttonView.setImageBitmap(image)
//            Toast.makeText(challengeCreateFm.requireActivity(),"$position",Toast.LENGTH_SHORT).show()
            buttonView.icon = BitmapDrawable(challengeCreateFm.requireContext().resources, image)
        }

        fun 백터이미지비트맵으로변환():Bitmap{
            var drawable = ContextCompat.getDrawable(
                challengeCreateFm.requireContext(),
                R.drawable.ic_baseline_circle_24
            )
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
                drawable = DrawableCompat.wrap(drawable!!).mutate()
            }
            val bitmap = Bitmap.createBitmap(
                drawable!!.intrinsicWidth,
                drawable!!.intrinsicHeight,
                Bitmap.Config.ARGB_8888
            )
            val canvas: Canvas = Canvas(bitmap)
            drawable.setBounds( 0, 0, canvas.getWidth(), canvas.getHeight())
            drawable.draw(canvas)

            return bitmap
        }


    }
}