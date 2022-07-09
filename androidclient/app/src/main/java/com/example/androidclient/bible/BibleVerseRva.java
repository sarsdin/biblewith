package com.example.androidclient.bible;

import android.graphics.Color;
import android.graphics.Paint;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.RecyclerView;

import com.example.androidclient.R;
import com.example.androidclient.databinding.BibleVerseFmVhBinding;
import com.google.android.material.bottomsheet.BottomSheetBehavior;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class BibleVerseRva extends RecyclerView.Adapter<BibleVerseRva.BibleVerseVh> {

    private BibleVm bibleVm;
    public BibleVerseFm bibleVerseFm;
    private List<BibleDto> list;
    private int position; //뷰홀더 포지션(위치)

    public BibleVerseRva(BibleVm bibleVm, BibleVerseFm bibleVerseFm) {
        this.bibleVm = bibleVm;
        this.bibleVerseFm = bibleVerseFm;
    }

    @Override
    public BibleVerseVh onCreateViewHolder(ViewGroup parent, int viewType) {
//        Log.e("[BibleVerseRva]", "onCreateViewHolder "+ holderSize++);
        BibleVerseFmVhBinding binding = BibleVerseFmVhBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);

        return new BibleVerseVh(binding);
    }

    @Override
    public void onBindViewHolder(final BibleVerseVh holder, int position) {
        int safePosition = holder.getBindingAdapterPosition();
//        int safePosition = holder.getAbsoluteAdapterPosition();
        holder.bind(bibleVm.verseL.get(safePosition));
//        Log.e("[BibleVerseRva]", "onBindViewHolder "+ holder.getAbsoluteAdapterPosition());
    }

    @Override
    public int getItemCount() {
        return bibleVm.verseL.size();
    }

    @Override
    public void onViewAttachedToWindow(@NonNull BibleVerseVh holder) {
        holder.setIsRecyclable(false); //홀더뷰 재사용 안함으로 설정하여 홀더의 절 ui들이 독립적으로 작동하게 함
        super.onViewAttachedToWindow(holder);
//        Log.e("[BibleBookRav]", "onViewAttachedToWindow "+ holder.getAbsoluteAdapterPosition());
    }

    public class BibleVerseVh extends RecyclerView.ViewHolder {
        public final TextView verseNo;
        public final TextView verseContent;
        public BibleDto mItem;
        BibleVerseFmVhBinding binding;

        public BibleVerseVh(BibleVerseFmVhBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
            verseNo = binding.verseNo;
            verseContent = binding.verseContent;
        }


        public void bind(BibleDto mItem) {
            this.mItem = mItem;
            //절의 번호는 먼저 ui적용
            binding.verseNo.setText(String.valueOf(mItem.getVerse()));
            //절의 내용은 하이라이트 존재 유무에 따라 색깔 적용
            if (mItem.getHighlight_color() != 0 ) {
                String reText = String.format("<span style='background-color:%s'>%s</span>" ,mItem.getHighlight_color(), mItem.getContent());
                binding.verseContent.setText(Html.fromHtml(reText, Html.FROM_HTML_MODE_LEGACY));

            } else {
                binding.verseContent.setText(mItem.getContent());
            }

            //선택된 것들은 선택 표시 Ui 갱신처리
            if (mItem.getHighlight_selected()) {
                binding.verseContent.setPaintFlags( binding.verseContent.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG); //텍스트 밑줄 처리
//                binding.verseContent.setText(binding.verseContent.getText());
//                binding.verseContent.setTextColor(Color.parseColor("#000000"));
            } else {
                binding.verseContent.setPaintFlags( Paint.HINTING_OFF);
//                binding.verseContent.setTextColor(Color.parseColor("#FF0855"));
//                String reText = String.format("<u>%s</u>" , binding.verseContent.getText());
//                binding.verseContent.setText(Html.fromHtml(reText, Html.FROM_HTML_MODE_LEGACY));
            }


            //홀더 클릭시 처리될 사항들
            binding.getRoot().setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
//                    position = getAbsoluteAdapterPosition(); //현재 홀더의 절대위치 LayoutManager를 이용하는 기능이라면 이것의 위치를 사용해야한다.
                    position = getLayoutPosition();
                    if (position != RecyclerView.NO_POSITION   ) {

                        //절 클릭시 바텀시트뷰 나타나게 함
                        bibleVerseFm.btsb.setState(BottomSheetBehavior.STATE_EXPANDED);
                        //바텀시트뷰 나타남과 동시에 뷰페이저 스와이프 막음
                        ((BibleFm) bibleVerseFm.getParentFragment()).binding.bibleTabLayoutViewpager.setUserInputEnabled(false);
                        //그리고 색 선택기 현재 절의 색으로 맞춰줌
                        bibleVm.colorL.get(0).setHighlight_color(mItem.getHighlight_color());
                        bibleVerseFm.btsR.getAdapter().notifyDataSetChanged();

                        //클릭하면 일단 선택됐는지 여부는 먼저 저장되야함. 토글로 처리
                        if (mItem.getHighlight_selected()) {
                            mItem.setHighlight_selected(false);
                        } else {
                            mItem.setHighlight_selected(true);
                        }

                        //지금 절 데이터를 highL 목록에 추가해야함. 물론, 현재 선택된 절인지 중복 검사는 해야함.
                       /* for (BibleDto item : bibleVm.highL ) {
                            if (item.getVerse() != mItem.getVerse()) {
                                bibleVm.highL.add(mItem);
//                                String format = String.format("scrollX: %d, Y: %d, oldX: %d, oldY: %d",binding.verseContent.getText());
                                String reText = String.format("<span style='background-color:%s'>%s</span>" ,
                                        bibleVm.colorL.get(0).getHighlight_color(), binding.verseContent.getText());
                                binding.verseContent.setText(Html.fromHtml(reText, Html.FROM_HTML_MODE_LEGACY));
//                                mItem.setHighlight_color();
                            }
                        }

                        //sort 작업해야함. 내림차순
                        Collections.sort(bibleVm.highL, new Comparator<BibleDto>() {
                            @Override
                            public int compare(BibleDto o1, BibleDto o2) {
                                if (o1.getVerse() > o2.getVerse()){
                                    return 1;
                                } else if (o1.getVerse() < o2.getVerse()) {
                                    return -1;       //안바꿈
                                }
                                return 0;
                            }
                        });*/



                        bibleVm.절번호업데이트( mItem.getVerse() );
                        //click event 등 안에 갱신 명령어가 있으면 Cannot call this method while RecyclerView is computing a layout 이 뜨지 않는가?
                        // onbindviewholder() 메소드안에 실행되는 notifyDataSetChanged(); 를 주석처리하면 안뜸.
                        notifyDataSetChanged();
//                        Navigation.findNavController(v).navigate(R.id.action_global_myHighLightBts);
                    }
                }
            });

        }
    }
}