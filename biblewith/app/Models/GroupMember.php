<?php

namespace App\Models;

class GroupMember extends \CodeIgniter\Model
{
    protected $table = 'GroupMember';
    protected $allowedFields = [
        'group_no',
        'user_no',
        'user_grade',
        'join_date',
        'exit_date',
        'group_member_no'
    ];
    protected $primaryKey = 'group_member_no';
}
