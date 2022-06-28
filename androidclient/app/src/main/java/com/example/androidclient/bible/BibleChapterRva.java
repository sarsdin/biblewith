package com.example.androidclient.bible;

import android.graphics.Typeface;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.androidclient.MyApp;
import com.example.androidclient.R;
import com.example.androidclient.databinding.BibleChapterFmVhBinding;

import java.util.List;
public class BibleChapterRva extends RecyclerView.Adapter<BibleChapterRva.BibleChapterVh> {

    private final BibleChapterFm bibleChapterFm;
    private BibleVm bibleVm;
    private List<BibleDto> list;
    private int position; //뷰홀더 포지션(위치)

    public BibleChapterRva(BibleVm vm, BibleChapterFm bibleChapterFm) {
        this.bibleVm = vm;
        this.bibleChapterFm = bibleChapterFm;
    }

    @Override
    public BibleChapterRva.BibleChapterVh onCreateViewHolder(ViewGroup parent, int viewType) {
        BibleChapterFmVhBinding binding = BibleChapterFmVhBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);

        return new BibleChapterRva.BibleChapterVh(binding);
    }

    @Override
    public void onBindViewHolder(BibleChapterRva.BibleChapterVh holder, int position) {
//        holder.mItem = mValues.get(position);
//        holder.mIdView.setText(mValues.get(position).id);
//        holder.mContentView.setText(mValues.get(position).content);
        holder.bind(bibleVm.chapterL.get(position));
    }

    @Override
    public int getItemCount() {
        return bibleVm.chapterL.size();
//        return mValues.size();
    }

    @Override
    public int getItemViewType(int position) {
        return super.getItemViewType(position);
    }

    @Override
    public void onViewAttachedToWindow(@NonNull BibleChapterVh holder) {
        holder.setIsRecyclable(false);
        super.onViewAttachedToWindow(holder);
//        Log.e("[BibleChapterRva]", "onViewAttachedToWindow "+ holder.getAbsoluteAdapterPosition());
    }






    public class BibleChapterVh extends RecyclerView.ViewHolder {
        public final TextView chapterNumber;
        public BibleDto mItem;
        BibleChapterFmVhBinding binding;

        public BibleChapterVh(BibleChapterFmVhBinding binding) {
            super(binding.getRoot());
            chapterNumber = binding.chapterNumber;
            this.binding = binding;
        }

        public void bind(BibleDto mItem){
            this.mItem = mItem;
            binding.chapterNumber.setText(String.valueOf(mItem.getChapter()));

            if (mItem.isCurrentItem()) {
                binding.chapterNumber.setTypeface(Typeface.DEFAULT_BOLD);
                binding.chapterNumber.setTextColor(MyApp.getApplication().getColorStateList(R.color.book_rv));
            }


            binding.getRoot().setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Log.e("[BibleBookRav]", "책장번호[0]: " + bibleVm.책장번호[0] );
//                    position = getAbsoluteAdapterPosition(); //현재 홀더의 절대위치 LayoutManager를 이용하는 기능이라면 이것의 위치를 사용해야한다.
                    position = getLayoutPosition();
                    if (position != RecyclerView.NO_POSITION   ) {
                        //로딩바 보이기
                        ((BibleVerseFm) ( bibleChapterFm.getParentFragmentManager().findFragmentById(0))).binding.bibleVerseFmProgressbar.setVisibility(View.VISIBLE);
                        bibleVm.장번호업데이트( mItem.getChapter());


                        bibleVm.절목록가져오기(bibleVm.책장번호[0], bibleVm.책장번호[1]);
                        //홀더 클릭시 절탭으로 넘어가기
                        assert bibleChapterFm.getParentFragment() != null;
                        ((BibleFm) bibleChapterFm.getParentFragment()).binding.bibleTabLayoutViewpager.setCurrentItem(2);
                        //스크롤 제일 위로 가기
//                        ((BibleVerseFm) ( bibleChapterFm.getParentFragmentManager().getFragments().get(2))).recyclerView.scrollToPosition(0);
//                        Log.e("[BibleBookRav]", "getParentFragmentManager: " +  bibleChapterFm.getParentFragmentManager().getFragments().get(0).getId() );
                        ((BibleVerseFm) ( bibleChapterFm.getParentFragmentManager().findFragmentById(0))).recyclerView.scrollToPosition(0);

                        notifyDataSetChanged();

                    }
                }
            });




        }

    }
}