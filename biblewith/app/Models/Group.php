<?php

namespace App\Models;

class Group extends \CodeIgniter\Model
{
    protected $table = 'Group';
    protected $allowedFields = [
        'group_no',
        'chat_room_no',
        'user_no',
        'group_name',
        'group_desc',
        'group_main_image',
        'create_date'
    ];
    protected $primaryKey = 'group_no';
}

