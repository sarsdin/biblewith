package jm.preversion.biblewith.bible;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.Lifecycle;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import java.util.List;

public class BibleVpa extends FragmentStateAdapter {

    public List<Fragment> pageFmList;


    public BibleVpa(List<Fragment> pageFmList, @NonNull FragmentManager fragmentManager, @NonNull Lifecycle lifecycle) { //childFragmentManager
        super(fragmentManager, lifecycle);
        this.pageFmList = pageFmList;
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        return pageFmList.get(position);
    }

    @Override
    public int getItemCount() {
        return pageFmList.size();
    }
}
