<?php

namespace App\Models;

class User extends \CodeIgniter\Model
{
    protected $table = 'User';
    protected $allowedFields = ['user_email', 'user_pwd', 'user_nick', 'user_create_date', 'user_name', 'user_image'];
    protected $primaryKey = 'user_no';
    protected $validationRules = [
//        'user_id' => 'required|alpha_numeric_space|min_length[3]|is_unique[users.user_id]',
        'user_email'=> 'required|valid_email|is_unique[User.user_email]',
        'user_pwd'=> 'required|min_length[7]',
        'user_pwdc'=> 'required|matches[user_pwd]',
    ];
    protected $validationMessages = [
//        'user_id' => [
//            'required' => '아이디를 입력해주세요.',
//            'min_length' => '아이디는 4자리 이상이어야 합니다.',
//            'alpha_numeric_space' => '아이디는 숫자,문자의 조합이며 공백은 포함될 수 없습니다.',
//            'is_unique' => '이미 사용중인 아이디입니다.'
//        ],
        'user_email'=> [
            'required' => '이메일을 입력해주세요.',
            'is_unique' => '이미 사용중인 이메일입니다.',
            'valid_email' => '이메일 형식이 맞지 않습니다.'
        ],
        'user_pwd'=> [
            'required' => '비밀번호를 입력해주세요.',
            'min_length' => '비밀번호는 8자리 이상이어야 합니다.',
        ],
        'user_pwdc'=> [
            'required' => '비밀번호를 입력해주세요.',
            'matches' => '비밀번호가 동일하지 않습니다.',
        ],
    ];



}