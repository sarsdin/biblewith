package jm.preversion.biblewith.login;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import jm.preversion.biblewith.home.HomeVm;

/**
 * ViewModel provider factory to instantiate LoginViewModel.
 * Required given LoginViewModel has a non-empty constructor
 */
public class LoginViewModelFactory implements ViewModelProvider.Factory {

    @NonNull
    @Override
    @SuppressWarnings("unchecked")
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
        if (modelClass.isAssignableFrom(LoginVm.class)) {
            return (T) new LoginVm(new LoginRepository());

        } else if (modelClass.isAssignableFrom(HomeVm.class)){
            return (T) new HomeVm();

        } else {
            throw new IllegalArgumentException("Unknown ViewModel class");
        }
    }
}