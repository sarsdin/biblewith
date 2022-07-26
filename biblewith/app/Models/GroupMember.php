<?php

namespace App\Models;

class GroupMember extends \CodeIgniter\Model
{
    protected $table = 'GroupMember';
    protected $allowedFields = [
        'group_no',
        'user_no',
    ];
    protected $primaryKey = 'group_no';
}
