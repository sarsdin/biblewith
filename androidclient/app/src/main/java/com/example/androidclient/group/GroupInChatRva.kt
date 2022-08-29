package com.example.androidclient.group

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.drawable.BitmapDrawable
import android.os.Build
import android.view.*
import androidx.recyclerview.widget.RecyclerView
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import androidx.navigation.Navigation
import com.example.androidclient.MyApp
import com.example.androidclient.R
import com.example.androidclient.databinding.GroupChatFmVhBinding

import com.example.androidclient.moreinfo.MyNoteRvaInner
import com.example.androidclient.util.ImageHelper
import com.google.android.material.button.MaterialButton
import com.google.gson.JsonObject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class GroupInChatRva(val groupVm: GroupVm, val groupInChatFm: GroupInChatFm) : RecyclerView.Adapter<GroupInChatRva.GroupChatFmVh>() {
    val tagName = "[GroupInChatRva]"

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GroupInChatRva.GroupChatFmVh {
        return GroupChatFmVh(GroupChatFmVhBinding.inflate(LayoutInflater.from(parent.context), parent,false))
    }

    override fun onBindViewHolder(holder: GroupInChatRva.GroupChatFmVh, position: Int) {
        holder.bind(groupVm.chatRoomInfoL[position] as JsonObject)
    }

    override fun getItemCount(): Int {
        if(groupVm.chatRoomInfoL.isJsonNull || groupVm.chatRoomInfoL.size() == 0){
            return 0
        }
        return groupVm.chatRoomInfoL.size()
    }



    inner class GroupChatFmVh(var binding: GroupChatFmVhBinding) : RecyclerView.ViewHolder(binding.root) {
        var rva: MyNoteRvaInner? = null
        var rv: RecyclerView? =null

        init {
        }

        //mItem -
        fun bind(mItem: JsonObject) {
//            this.mItem = mItem;

            ImageHelper.getImageUsingGlide(groupInChatFm.requireContext(), mItem.get("chat_room_image").asString, binding.chatIv)
            binding.chatTitle.text = mItem.get("chat_room_title").asString
            if (mItem.get("last_msg_date") != null && !mItem.get("last_msg_date").isJsonNull) {
                binding.chatDate.text = MyApp.getTime("ui4", mItem.get("last_msg_date").asString)
            }
            else {
                binding.chatDate.text = MyApp.getTime(".", mItem.get("create_date").asString) //아무런 메시지가 없으면 방생성일 보여줌
            }

            // todo 여기에 방에 참가했는지 여부를 체크하고 안했으면 안했다는 메시지 분기문을 짜야함
            // 현재 뷰홀더가 나타내고 있는 채팅방에 내가 참가했는지 여부를 검사한다. 1이면 참가했다는 것! 0이면 아님.
            if (mItem.get("my_is_joined") != null && mItem.get("my_is_joined").asInt == 1) {
                if (mItem.get("last_user_nick") != null && !mItem.get("last_user_nick").isJsonNull){
                    if(!mItem.get("last_chat_content").isJsonNull){
                        if(!mItem.get("last_chat_type").isJsonNull){
                            when (mItem.get("last_chat_type").asString) {
                                "이미지" -> {
                                    binding.lastChat.text = "${mItem.get("last_user_nick").asString}: 이미지"

                                }
                                "접속알림" -> {
                                    binding.lastChat.text = "${mItem.get("last_user_nick").asString}: 접속알림"

                                }
                                else -> {
                                    binding.lastChat.text = "${mItem.get("last_user_nick").asString}: ${mItem.get("last_chat_content").asString}"
                                }
                            }
                        }
                    }
                }

            //그래서, 채팅방에 참가 안했으면 참가안했다는 메세지를 보여줘야함!
            } else {
                binding.lastChat.text = "\uD83D\uDE38 방에 참가해 모임원들과 소통하세요!"
            }


            //마지막 채팅내용을 실시간으로 표시해야하는데, 백그라운드의 소켓통신 스트림의 받은 채팅내용을 채팅리스트변수에 업데이트 시켜줌과 동시에
//            binding.lastChat.text = mItem.asJsonObject.get("chat_room_title").asString
//            binding.chatDate.text = mItem.asJsonObject.get("chat_room_title").asString
            //알림의 읽지않은 채팅 개수 유무도 표시해야함 - 숫자: 서버에서 읽지않은 레코드를 계산된 것으로 가져와야할듯?
            if(mItem.get("my_unread_count") != null && mItem.get("my_unread_count").asInt != 0) {
                숫자이미지넣기(binding.notiEa, mItem.get("my_unread_count").asInt, groupInChatFm.requireContext())
                binding.notiEa.visibility = View.VISIBLE
            } else {
                binding.notiEa.visibility = View.GONE
            }

            //채팅방 클릭시 - 참가하기
            binding.root.setOnClickListener {
//                setProgressDialog(groupInChatFm.requireContext())
                CoroutineScope(Dispatchers.Main).launch {
                    groupVm.채팅방참가클릭(mItem.get("chat_room_no").asInt,
                        MyApp.userInfo.user_no,
                        groupVm.groupInfo.get("group_no").asInt,
                        true)
                    //화면 이동할때 쓰이는 라이프사이클 관련 메소드(setCurrentState)는 navigation에 연관되는듯.
                    // 그래서 Method setCurrentState must be called on the main thread 예외가 뜬다. IO는 다른쓰레드풀에서 작동하기에 Main에서 돌려준다.
                    Navigation.findNavController(it).navigate(R.id.action_global_groupChatInnerFm)
                }
            }

        }


        //숫자를 드로어블로 표현할 뷰객체, 거기에 넣을 숫자
        fun 숫자이미지넣기(view: ImageView, position: Int, context:Context){
            val fontSize = if(position < 100) 40 else 28
//            val baseImage = BitmapFactory.decodeResource(challengeCreateFm.requireContext().resources, R.drawable.ic_baseline_circle_24) //load the hexagon image
//            val baseImage = AppCompatResources.getDrawable(challengeCreateFm.requireContext(), R.drawable.ic_baseline_circle_24)!!
//                .toBitmap(50, 50)
//            val image = baseImage.copy(Bitmap.Config.ARGB_8888,true) //create mutable bitmap for canvas. see: http://stackoverflow.com/a/13119762/1896516
            val image = 백터이미지비트맵으로변환(context)

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
            view.setImageDrawable(BitmapDrawable(context.resources, image))
        }

        fun 백터이미지비트맵으로변환(context:Context): Bitmap {
            var drawable = ContextCompat.getDrawable(
                context,
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


        // Function to display ProgressBar
        // inside AlertDialog
        @SuppressLint("SetTextI18n")
        fun setProgressDialog(context: Context) {
            // Creating a Linear Layout
            val llPadding = 30
            val ll = LinearLayout(context)
            ll.orientation = LinearLayout.HORIZONTAL
            ll.setPadding(llPadding, llPadding, llPadding, llPadding)
            ll.gravity = Gravity.CENTER
            var llParam = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            llParam.gravity = Gravity.CENTER
            ll.layoutParams = llParam
            ll.setBackgroundResource(0) //백그라운드 제거

            // Creating a ProgressBar inside the layout
            val progressBar = ProgressBar(context)
            progressBar.isIndeterminate = true
            progressBar.setPadding(0, 0, llPadding, 0)
            progressBar.layoutParams = llParam
            llParam = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            llParam.gravity = Gravity.CENTER

            // Creating a TextView inside the layout
            val tvText = TextView(context)
            tvText.text = "채팅방에 들어가는 중입니다 ..."
            tvText.setTextColor(Color.parseColor("#000000"))
            tvText.textSize = 20f
            tvText.layoutParams = llParam
            ll.addView(progressBar)
            ll.addView(tvText)

            // Setting the AlertDialog Builder view
            // as the Linear layout created above
            val builder: AlertDialog.Builder = AlertDialog.Builder(context)
            builder.setCancelable(true)
            builder.setView(ll)

            // Displaying the dialog
            val dialog: AlertDialog = builder.create()
            dialog.show()

            val window: Window? = dialog.window
            if (window != null) {
                val layoutParams = WindowManager.LayoutParams()
                layoutParams.copyFrom(dialog.window?.attributes)
                layoutParams.width = LinearLayout.LayoutParams.WRAP_CONTENT
                layoutParams.height = LinearLayout.LayoutParams.WRAP_CONTENT
                dialog.window?.attributes = layoutParams

                // Disabling screen touch to avoid exiting the Dialog
                window.setFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE, WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
            }
        }




    }
}