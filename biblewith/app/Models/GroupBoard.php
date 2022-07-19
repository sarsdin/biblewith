<?php

namespace App\Models;

class GroupBoard extends \CodeIgniter\Model
{
    protected $table = 'GroupBoard';
    protected $allowedFields = [
        'gboard_no',
        'group_no',
        'user_no',
        'gboard_title',
        'gboard_content',
        'create_date'
    ];
    protected $primaryKey = 'gboard_no';
}

