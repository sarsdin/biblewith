<?php

namespace App\Models;

class Reply extends \CodeIgniter\Model
{
    protected $table = 'Reply';
    protected $allowedFields = [
        'reply_no',
        'gboard_no',
        'user_no',
        'reply_content',
        'reply_writedate',
        'reply_deletedate',
        'parent_reply_no',
        'parent_reply_writer_no',
        'reply_group'
    ];
    protected $primaryKey = 'reply_no';
}







