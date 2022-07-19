<?php

namespace App\Models;

class GroupNotification extends \CodeIgniter\Model
{
    protected $table = 'GroupNotification';
    protected $allowedFields = [
        'notification_no',
        'group_no',
        'user_no',
        'notification_type',
        'user_no_cause',
        'notification_target_no',
        'notification_content',
        'create_date',
        'read_date'
    ];
    protected $primaryKey = 'notification_no';
}
