<?php

namespace App\Models;

class ChallengeDetailLike extends \CodeIgniter\Model
{
    protected $table = 'ChallengeDetailLike';
    protected $allowedFields = [
        'chal_like_no',
        'chal_detail_no',
        'user_no',
        'like_bt_no',
        'is_checked'
    ];
    protected $primaryKey = 'chal_like_no';
}



