<?php

namespace App\Models;

class GboardLike extends \CodeIgniter\Model
{
    protected $table = 'GboardLike';
    protected $allowedFields = [
        'gboard_like_no',
        'gboard_no',
        'user_no'
    ];
    protected $primaryKey = 'gboard_like_no';
}



