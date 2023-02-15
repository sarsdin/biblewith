package com.burhanrashid52.photoediting

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.LinearLayoutManager
import com.burhanrashid52.photoediting.ColorPickerAdapter.OnColorPickerClickListener
import kotlin.jvm.JvmOverloads
import androidx.appcompat.app.AppCompatActivity
import androidx.annotation.ColorInt
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment

/**
 * Created by Burhanuddin Rashid on 1/16/2018.
 */
class TextEditorDialogFragment : DialogFragment() {
    private var mAddTextEditText: EditText? = null
    private var mAddTextDoneTextView: TextView? = null
    private var mInputMethodManager: InputMethodManager? = null
    private var mColorCode = 0
    private var mTextEditorListener: TextEditorListener? = null

    interface TextEditorListener {
        fun onDone(inputText: String?, colorCode: Int)
    }

    override fun onStart() {
        super.onStart()
        val dialog = dialog
        //Make dialog full screen with transparent background
        if (dialog != null) {
            val width = ViewGroup.LayoutParams.MATCH_PARENT
            val height = ViewGroup.LayoutParams.MATCH_PARENT
            dialog.window!!.setLayout(width, height)
            dialog.window!!.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.add_text_dialog, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mAddTextEditText = view.findViewById(R.id.add_text_edit_text)
        mInputMethodManager =
            activity!!.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        mAddTextDoneTextView = view.findViewById(R.id.add_text_done_tv)

        //Setup the color picker for text color
        val addTextColorPickerRecyclerView: RecyclerView =
            view.findViewById(R.id.add_text_color_picker_recycler_view)
        val layoutManager = LinearLayoutManager(activity, LinearLayoutManager.HORIZONTAL, false)
        addTextColorPickerRecyclerView.layoutManager = layoutManager
        addTextColorPickerRecyclerView.setHasFixedSize(true)
        val colorPickerAdapter = ColorPickerAdapter(activity!!)

        //This listener will change the text color when clicked on any color from picker
        colorPickerAdapter.setOnColorPickerClickListener(object : OnColorPickerClickListener {
            override fun onColorPickerClickListener(colorCode: Int) {
                mColorCode = colorCode
                mAddTextEditText!!.setTextColor(colorCode)
            }
        })
        addTextColorPickerRecyclerView.adapter = colorPickerAdapter
        mAddTextEditText!!.setText(arguments!!.getString(EXTRA_INPUT_TEXT))
        mColorCode = arguments!!.getInt(EXTRA_COLOR_CODE)
        mAddTextEditText!!.setTextColor(mColorCode)
        mInputMethodManager!!.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0)

        //Make a callback on activity when user is done with text editing
        //Done 버튼 눌렀을때 실행될 명령들
        mAddTextDoneTextView!!.setOnClickListener { onClickListenerView ->
            mInputMethodManager!!.hideSoftInputFromWindow(onClickListenerView.windowToken, 0)
            dismiss() //텍스트 다이얼로그 프래그먼트를 일단 끄고 mAddTextEditText(방금 다이얼로그에서 입력한 텍스트를 inputText 변수에 담고
            //비어있지 않다면! -> 현재 등록된 리스너(메소드: 액티비티에 기술된 인터페이스의 onDone 을 실행함)
            val inputText = mAddTextEditText!!.text.toString()
            if (!TextUtils.isEmpty(inputText) && mTextEditorListener != null) {
                mTextEditorListener!!.onDone(inputText, mColorCode) //액티비티에서 프래그먼트show()만들고 리스너 등록할때 구현된 onDone() 부분 실행!
            }
        }

    }

    fun 완료실행(){
        mAddTextDoneTextView?.performClick()
    }

    //Callback to listener if user is done with text editing
    //TextEditorDialogFragment.show() 후 만들어진 프래그먼트 인스턴스에서 onDone()메소드를 구현하는 익명 리스너 객체를 전달받아 (프래그먼트 인스턴스에)등록한다
    fun setOnTextEditorListener(textEditorListener: TextEditorListener) { //액티비티의 onEditTextChangeListener() 시 실행(등록)되어진다
        mTextEditorListener = textEditorListener
    }

    companion object {
        //여기서 각 프래그먼트의 TAG 명을 정해준다.
        private val TAG: String = TextEditorDialogFragment::class.java.simpleName
        const val EXTRA_INPUT_TEXT = "extra_input_text"
        const val EXTRA_COLOR_CODE = "extra_color_code"

        //Show dialog with provide text and text color
        //Show dialog with default text input as empty and text color white
        //show 가 실행되면 초기화된 TextEditorDialogFragment 인스턴스가 하나 만들어지고 그 인스턴스를 호출한 액티비티등에 리턴한다.
        //리턴전에 일단 show() 를 이용해 이 만들어진 dialogFragment 를 액티비티 위에 보여준다.
        @JvmOverloads
        fun show(
            appCompatActivity: AppCompatActivity,
            inputText: String = "",
            @ColorInt colorCode: Int = ContextCompat.getColor(appCompatActivity, R.color.white)
        ): TextEditorDialogFragment {
            val args = Bundle()
            args.putString(EXTRA_INPUT_TEXT, inputText) //프래그먼트에 초기화된 정보 "extra_input_text" 와
            args.putInt(EXTRA_COLOR_CODE, colorCode)    //프래그먼트에 초기화된 정보 "extra_color_code" 를 추가해줌
            val fragment = TextEditorDialogFragment()
            fragment.arguments = args                   //인스턴스된 프래그먼트에 위에서 만든 번들 추가
            fragment.show(appCompatActivity.supportFragmentManager, TAG) //이 프래그먼트를 호출한 액티비티 위에 대화상자 프래그먼트를 표시(보여줌)
            return fragment         //호출된 곳에 프래그먼트 리턴!
        }
    }
}