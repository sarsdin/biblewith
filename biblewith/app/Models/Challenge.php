<?php

namespace App\Models;

class Challenge extends \CodeIgniter\Model
{
    protected $table = 'Challenge';
    protected $allowedFields = [
        'chal_no',
        'user_no',
        'chal_title',
        'chal_create_date',
        'isclear',
        'group_no'
    ];
    protected $primaryKey = 'chal_no';
}


