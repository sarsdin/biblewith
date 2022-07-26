package com.example.androidclient.group

import android.graphics.*
import android.graphics.drawable.BitmapDrawable
import android.os.Build
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.recyclerview.widget.RecyclerView
import com.example.androidclient.R
import com.example.androidclient.databinding.ChallengeCreateFmVhBinding
import com.example.androidclient.moreinfo.MyNoteRvaInner
import com.google.android.material.button.MaterialButton
import com.google.gson.JsonObject


class ChallengeCreateFmRva(val groupVm: GroupVm, val challengeCreateFm: ChallengeCreateFm)
    : RecyclerView.Adapter<ChallengeCreateFmRva.ChallengeCreateFmVh>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChallengeCreateFmRva.ChallengeCreateFmVh {
        return ChallengeCreateFmVh(ChallengeCreateFmVhBinding.inflate(LayoutInflater.from(parent.context), parent,false))
    }

    override fun onBindViewHolder(holder: ChallengeCreateFmVh, position: Int) {
        holder.bind(groupVm.groupL[position] as JsonObject)
    }

    override fun getItemCount(): Int {
        return groupVm.groupL.size()
    }


    inner class ChallengeCreateFmVh(var binding: ChallengeCreateFmVhBinding) : RecyclerView.ViewHolder(binding.root) {
        var rva: MyNoteRvaInner? = null
        var rv: RecyclerView? =null

        init {
        }

        //mItem -- 모임목록가져오기()
        fun bind(mItem: JsonObject) {
//            this.mItem = mItem;
//            binding.groupIvInCardview.setImageURI(Uri.parse(UPLOADS_URL + mItem.get("group_main_image").asString))
//            binding.createCkboxVh
            숫자이미지넣기(binding.createCkboxVh)


        }

        fun 숫자이미지넣기(buttonView: MaterialButton){
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
            canvas.drawText("22", (image.width / 2).toFloat(),
                (image.height / 2 + fontSize / 4).toFloat()+3.5f, paint) //draw the number onto the hexagon
//            val iv: ImageView = findViewById(com.example.androidclient.R.id.iv) as ImageView
//            buttonView.setImageBitmap(image)

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