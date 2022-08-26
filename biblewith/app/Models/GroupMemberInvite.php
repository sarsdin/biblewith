<?php

namespace App\Models;

class GroupMemberInvite extends \CodeIgniter\Model
{
    protected $table = 'GroupMemberInvite';
    protected $allowedFields = [
        'invite_no',
        'group_no',
        'invite_code',
        'expire_date',
        'code_type'
    ];
    protected $primaryKey = 'invite_no';
}
